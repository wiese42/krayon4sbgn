/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.utils.IEventListener
import com.yworks.yfiles.view.input.GraphEditorInputMode
import com.yworks.yfiles.view.input.ICommand
import com.yworks.yfiles.view.input.IEventRecognizer
import com.yworks.yfiles.view.input.InputModeEventArgs
import krayon.editor.base.util.geim

object InteractivePaste : ApplicationCommand("INTERACTIVE_PASTE") {
    override fun execute(param: Any?) {
        val location = param as? PointD ?: graphComponent.lastMouse2DEvent.location
        val ceim = graphComponent.geim
        ICommand.PASTE.execute(location, graphComponent)
        cleanupPorts()
        when {
            param == false || param is PointD && param != graphComponent.lastMouse2DEvent.location -> {
                //use specific location.
            }
            ceim.mutexOwner == ceim.waitInputMode -> {
                var waitListener: IEventListener<InputModeEventArgs?>? = null
                waitListener = IEventListener { _, _ ->
                    ceim.waitInputMode.removeWaitingEndedListener(waitListener)
                    doStartDragAfterPaste(location)
                }
                ceim.waitInputMode.addWaitingEndedListener(waitListener)
            }
            ceim.mutexOwner == null ->
                doStartDragAfterPaste(location)
            else -> {
                //we don't have the mutex and have no clue how to obtain it
                println("unknown mutex owner: ${ceim.mutexOwner}")

            }
        }
    }

    private fun cleanupPorts() {
        for (node in selectedNodes) {
            node.ports.filter { graph.degree(it) == 0 && it.tag == null }.forEach(graph::remove)
        }
    }

    override fun canExecute(param: Any?): Boolean {
        //println("canExecute=" + !graphComponent.clipboard.isEmpty)
        return !graphComponent.clipboard.isEmpty
    }

    private fun doStartDragAfterPaste(location: PointD) {
        with((graphComponent.inputMode as GraphEditorInputMode).moveInputMode) {
            val prevDraggedRecognizer = draggedRecognizer
            draggedRecognizer = IEventRecognizer.MOUSE_MOVED
            var cleanup: IEventListener<InputModeEventArgs?>? = null
            cleanup = IEventListener { _, _ ->
                draggedRecognizer = prevDraggedRecognizer
                removeDragFinishingListener(cleanup)
                removeDragCanceledListener(cleanup)
            }
            addDragFinishingListener(cleanup)
            addDragCanceledListener(cleanup)
            doStartDrag(location)
        }
    }
}

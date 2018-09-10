/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.utils.IEventListener
import com.yworks.yfiles.view.input.IEventRecognizer
import com.yworks.yfiles.view.input.INodeHitTester
import com.yworks.yfiles.view.input.InputModeEventArgs
import krayon.editor.base.command.ApplicationCommand
import krayon.editor.base.util.geim
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.orientation
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.SbgnBuilder

object CreateNode : ApplicationCommand("CREATE_NODE") {
    override fun canExecute(param: Any?): Boolean {
        return true
    }

    override fun execute(param: Any?) {
        val node = graph.createNode()
        node.type = SbgnType.SIMPLE_CHEMICAL
        SbgnBuilder.configure(graph, node)
        SbgnBuilder.addNameLabel(graph, node)
        val location = graphComponent.lastMouse2DEvent.location
        graph.setNodeLayout(node, RectD.fromCenter(location, SbgnBuilder.getPrevalentSize(graph, node.type, node.orientation, node) ?: node.layout.toSizeD()))

        with(graphComponent.geim.inputModeContext) {
            val parent = lookup(INodeHitTester::class.java)?.enumerateHits(this, location)?.firstOrNull{ graph.isGroupNode(it)}
            graph.setParent(node, parent)
        }

        graphComponent.selection.apply {
            clear()
            setSelected(node, true)
        }
        doStartMoveSelection(location)
    }

    private fun doStartMoveSelection(location: PointD) {
        with(graphComponent.geim.moveInputMode) {
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

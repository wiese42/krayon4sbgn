/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import com.yworks.yfiles.graph.AbstractUndoUnit
import com.yworks.yfiles.graph.INode
import krayon.editor.base.util.beginEdit
import krayon.editor.base.util.getMainCanvasObject

object DrawingOrderCommands {

    private abstract class DrawingOrderCommand(id:String) : ApplicationCommand(id) {
        override fun canExecute(param: Any?) = selectedNodes.any()

        override fun execute(param: Any?) {
            graph.beginEdit(id).use {
                selectedNodes.forEach {
                    graph.undoEngine.addUnit(DrawingOrderUndoUnit(it))
                    changeDrawingOrder(it)
                }
            }
        }

        abstract fun changeDrawingOrder(it: INode)

        fun getMainCanvasObject(node:INode) = graphComponent.graphModelManager.getMainCanvasObject(node)

        inner class DrawingOrderUndoUnit(val node: INode) : AbstractUndoUnit(id) {
            var index = graphComponent.graphModelManager.nodeGroup.indexOf(getMainCanvasObject(node))

            override fun redo() {
                changeDrawingOrder(node)
            }

            override fun undo() {
                val canvasObject = getMainCanvasObject(node)
                var currentIndex = graphComponent.graphModelManager.nodeGroup.indexOf(canvasObject)
                while(currentIndex-- > index) canvasObject.lower()
                while(currentIndex++ < index) canvasObject.raise()
            }
        }
    }

    val NodesToFront:ApplicationCommand = object:DrawingOrderCommand("NODES_TO_FRONT") {
        override fun changeDrawingOrder(it: INode) {
            getMainCanvasObject(it).toFront()
        }
    }

    val NodesToBack:ApplicationCommand = object:DrawingOrderCommand("NODES_TO_BACK") {
        override fun changeDrawingOrder(it: INode) {
            getMainCanvasObject(it).toBack()
        }
    }
}
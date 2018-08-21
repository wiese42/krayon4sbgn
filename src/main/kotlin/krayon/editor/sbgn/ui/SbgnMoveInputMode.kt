/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.graph.ICompoundEdit
import com.yworks.yfiles.view.input.InputModeEventArgs
import com.yworks.yfiles.view.input.MoveInputMode
import krayon.editor.base.util.beginEdit
import krayon.editor.base.util.graphComponent
import krayon.editor.sbgn.model.getNameLabel

class SbgnMoveInputMode : MoveInputMode() {
    private var activeEdit: ICompoundEdit? = null
    private val dragNodesManager = DragNodesManager()
    override fun onDragStarting(args: InputModeEventArgs) {
        activeEdit = inputModeContext.graph.beginEdit("Move Selected Items")
        super.onDragStarting(args)
    }

    override fun onDragStarted(args: InputModeEventArgs) {
        super.onDragStarted(args)
        with(args.context) { dragNodesManager.startDrag(this, graph, graphComponent.selection.selectedNodes) }
    }

    override fun onDragging(p0: InputModeEventArgs) {
        super.onDragging(p0)
        with(inputModeContext) { dragNodesManager.onDrag(this, graphComponent.lastMouse2DEvent.location) }
    }

    override fun onDragFinished(args: InputModeEventArgs) {
        super.onDragFinished(args)
        with(args.context) {
            val newLabelText = graphComponent.selection.selectedNodes.firstOrNull()?.getNameLabel()?.text
            dragNodesManager.onDrop(this, graphComponent.selection.selectedNodes, graphComponent.lastMouse2DEvent.location, null, newLabelText)
        }
        activeEdit?.commit()
        activeEdit = null
    }

    override fun onDragCanceled(args: InputModeEventArgs) {
        super.onDragCanceled(args)
        dragNodesManager.cleanup(args.context)
        activeEdit?.cancel()
        activeEdit = null
    }


}
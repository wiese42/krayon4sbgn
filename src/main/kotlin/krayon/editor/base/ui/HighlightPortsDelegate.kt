/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.utils.IEventListener
import com.yworks.yfiles.view.Mouse2DEventArgs
import com.yworks.yfiles.view.input.ConcurrencyController
import com.yworks.yfiles.view.input.HoveredItemChangedEventArgs
import com.yworks.yfiles.view.input.IInputModeContext
import krayon.editor.base.util.graphComponent

class HighlightPortsDelegate : MultiplexingNodeHoverInputMode.ItemHoverDelegate {

    private var highlightPortsSupport = HighlightPortsSupport()
    private var mouseMovedListener = IEventListener<Mouse2DEventArgs> { _, _ ->
        if(highlightPortsSupport.node != null) highlightPortsSupport.updatePortHighlights(highlightPortsSupport.node)
    }

    override fun install(context: IInputModeContext, controller: ConcurrencyController) {
        highlightPortsSupport.install(context)
        context.canvasComponent.addMouse2DMovedListener(mouseMovedListener)
    }

    override fun uninstall(context: IInputModeContext) {
        highlightPortsSupport.uninstall(context)
        context.canvasComponent.removeMouse2DMovedListener(mouseMovedListener)
    }

    override fun onHoveredItemChanged(args: HoveredItemChangedEventArgs) {
        highlightPortsSupport.deinstallPortHighlights()
        val node = args.item as? INode
        if(node != null && highlightPortsSupport.inputModeContext?.graphComponent?.selection?.isSelected(node) == false) {
            highlightPortsSupport.updatePortHighlights(node)
        }
    }

    override fun onConcurrencyControllerDeactivated() {
        highlightPortsSupport.deinstallPortHighlights()
    }

    override fun updateHoverState() {}
}
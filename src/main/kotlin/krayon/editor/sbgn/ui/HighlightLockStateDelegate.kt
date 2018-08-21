/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.InsetsD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.InteriorLabelModel
import com.yworks.yfiles.graph.styles.IconLabelStyle
import com.yworks.yfiles.view.input.ConcurrencyController
import com.yworks.yfiles.view.input.HoveredItemChangedEventArgs
import com.yworks.yfiles.view.input.IInputModeContext
import krayon.editor.base.ui.MultiplexingNodeHoverInputMode
import krayon.editor.base.util.ApplicationSettings
import krayon.editor.sbgn.model.SbgnPropertyKey
import krayon.editor.sbgn.model.isLocked
import krayon.editor.sbgn.model.setSbgnProperty
import krayon.editor.sbgn.model.type

class HighlightLockStateDelegate : MultiplexingNodeHoverInputMode.ItemHoverDelegate {

    private var context:IInputModeContext? = null

    private var lockedLabel:ILabel? = null
    private val lockedLabelStyle:IconLabelStyle
    private var hoverNode:INode? = null

    init {
        val iconPath = "${ApplicationSettings.CANVAS_ICON_PATH.value}/locked.png"
        lockedLabelStyle= IconLabelStyle(javaClass.getResource(iconPath)).apply {
            iconSize = SizeD(12.0,12.0)
            iconPlacement = InteriorLabelModel.SOUTH_EAST
        }
    }
    override fun install(context: IInputModeContext, controller: ConcurrencyController) {
        this.context = context
    }

    override fun uninstall(context: IInputModeContext) {
        removeLockedLabel()
    }

    override fun onConcurrencyControllerDeactivated() {
        //removeLockedLabel()
    }

    override fun onHoveredItemChanged(args: HoveredItemChangedEventArgs) {
        removeLockedLabel()
        val node = args.item as? INode
        hoverNode = node
        if(node != null && node.type.isComplex() && node.isLocked) {
            updateHoverState()
        }
    }

    private fun removeLockedLabel() {
        context?.graph?.apply {
            if(lockedLabel != null && contains(lockedLabel)) {
                remove(lockedLabel)
                lockedLabel = null
            }
        }
    }

    override fun updateHoverState() {
        if(hoverNode?.isLocked == false && lockedLabel != null) {
            removeLockedLabel()
        }
        else if(hoverNode?.isLocked == true && lockedLabel == null) {
            val param = InteriorLabelModel().apply { insets = InsetsD(15.0) }.createParameter(InteriorLabelModel.Position.NORTH_WEST)
            context?.graph?.addLabel(hoverNode, "", param, lockedLabelStyle)?.let {
                it.setSbgnProperty(SbgnPropertyKey.IS_LOCKED, true)
                lockedLabel = it
            }
        }
    }

}
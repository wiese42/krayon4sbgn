/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.graph.GraphItemTypes
import com.yworks.yfiles.graph.IModelItem
import com.yworks.yfiles.view.input.*

class MultiplexingNodeHoverInputMode : ItemHoverInputMode() {

    val delegates = mutableListOf<ItemHoverDelegate>()

    init {
        isInvalidItemsDiscardingEnabled = true
        hoverItems = GraphItemTypes.NODE
    }

    interface ItemHoverDelegate {
        fun install(context: IInputModeContext, controller: ConcurrencyController)
        fun uninstall(context: IInputModeContext)
        fun onConcurrencyControllerDeactivated()
        fun onHoveredItemChanged(args: HoveredItemChangedEventArgs)
        fun updateHoverState()
    }

    override fun install(p0: IInputModeContext, p1: ConcurrencyController) {
        delegates.forEach { it.install(p0, p1) }
        super.install(p0, p1)
    }

    override fun uninstall(p0: IInputModeContext) {
        delegates.forEach { it.uninstall(p0) }
        super.uninstall(p0)

    }

    override fun onConcurrencyControllerDeactivated() {
        delegates.forEach { it.onConcurrencyControllerDeactivated() }
        super.onConcurrencyControllerDeactivated()
    }

    override fun getHitItemsAt(p: PointD): MutableIterable<IModelItem> {
        return inputModeContext.lookup(INodeHitTester::class.java)?.enumerateHits(inputModeContext, p) ?: super.getHitItemsAt(p)
    }

    override fun isValidHoverItem(item: IModelItem): Boolean {
        return true
    }

    override fun onHoveredItemChanged(args: HoveredItemChangedEventArgs) {
        super.onHoveredItemChanged(args)
        delegates.forEach {
            it.onHoveredItemChanged(args)
        }
    }

    fun updateHoverEffects() {
        delegates.forEach {
            it.updateHoverState()
        }
    }
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import krayon.editor.base.style.IStyleableContext
import krayon.editor.base.style.StyleProperty

class NodeStyleForMultimerFeature : SbgnMacroMoleculeStyle() {

    //suppress style handling
    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {

    }
    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {

    }

    //always use graphComponent background as paint. simulates transparent look
    override fun createVisual(context: IRenderContext, node: INode): IVisual {
        paint = context.canvasComponent.background
        return super.createVisual(context, node)
    }

    override fun updateVisual(context: IRenderContext, visual: IVisual, node: INode): IVisual {
        paint = context.canvasComponent.background
        return super.updateVisual(context, visual, node)
    }
}
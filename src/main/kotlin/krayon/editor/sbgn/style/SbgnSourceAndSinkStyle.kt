/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.GeneralPath
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import com.yworks.yfiles.view.ShapeVisual
import com.yworks.yfiles.view.VisualGroup
import krayon.editor.base.style.GeneralPathNodeStyle
import krayon.editor.base.style.IStyleable
import java.awt.geom.Line2D

class SbgnSourceAndSinkStyle : GeneralPathNodeStyle(), IStyleable {

    override fun createGeneralPath(node: INode, size: SizeD): GeneralPath {
        return GeneralPath().apply {
            appendEllipse(RectD(PointD.ORIGIN, size), false)
        }
    }

    override fun createVisual(context: IRenderContext?, node: INode): IVisual {
        return VisualGroup().apply {
            add(super.createVisual(context, node))
            add(createLineVisual(node))
        }
    }

    private fun createLineVisual(node:INode) = with(node.layout) { ShapeVisual(Line2D.Double(x, y+height, x+width, y), pen, paint) }

    override fun updateVisual(context: IRenderContext, visual: IVisual, node: INode): IVisual {
        return (visual as VisualGroup).apply {
            children[0] = super.updateVisual(context, children[0], node)
            children[1] = createLineVisual(node)
        }
    }
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.IRenderContext
import krayon.editor.base.style.TextVisual
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Ellipse2D


class SbgnLogicalOperatorStyle(private var operatorType: Type = Type.AND): InputOutputNodeStyle() {

    override fun getCoreShape(node: INode, size: SizeD): Shape {
        return Ellipse2D.Double(0.0, 0.0, size.width, size.height)
    }

    override fun paintCore(context: IRenderContext, gfx: Graphics2D, node: INode, size: SizeD) {
        TextVisual(operatorType.label, font, fontColor, RectD(PointD.ORIGIN, size)).paint(context, gfx)
    }

    enum class Type(val label:String) {
        AND("AND") ,
        OR("OR"),
        NOT("NOT")
    }

}
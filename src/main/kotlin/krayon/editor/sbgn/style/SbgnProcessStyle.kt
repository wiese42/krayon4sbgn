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
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D

class SbgnProcessStyle(private var processType: Type = Type.DEFAULT) : InputOutputNodeStyle() {

    enum class Type {
        DEFAULT,
        OMITTED,
        UNCERTAIN,
        ASSOCIATION,
        DISSOCIATION
    }

    init {
        if(processType == Type.ASSOCIATION) {
            paint = Color.BLACK
        }
    }

    override fun getCoreShape(node: INode, size: SizeD): Shape {
        return when (processType) {
            Type.DEFAULT, Type.OMITTED, Type.UNCERTAIN -> Rectangle2D.Double(0.0,0.0, size.width, size.height)
            else -> Ellipse2D.Double(0.0,0.0, size.width, size.height)
        }
    }

    override fun paintCore(context: IRenderContext, gfx: Graphics2D, node:INode, size: SizeD) {
        when (processType) {
            Type.DEFAULT -> {

            }
            Type.OMITTED -> {
                TextVisual("\\\\", font, fontColor, RectD(PointD.ORIGIN, size)).paint(context, gfx)
            }
            Type.UNCERTAIN -> {
                TextVisual("?", font, fontColor, RectD(PointD.ORIGIN, size)).paint(context, gfx)
            }
            Type.ASSOCIATION -> {

            }
            Type.DISSOCIATION -> {
                val gap = size.width * 0.25
                paintShape(gfx, Ellipse2D.Double(gap, gap, size.width - 2.0 * gap, size.height - 2.0 * gap), pen, null)
            }
        }
    }
}
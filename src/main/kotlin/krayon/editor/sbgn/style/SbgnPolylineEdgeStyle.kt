/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.graph.IEdge
import com.yworks.yfiles.graph.styles.PolylineEdgeStyle
import com.yworks.yfiles.graph.styles.PolylineEdgeStyleRenderer
import com.yworks.yfiles.view.Pen
import krayon.editor.base.style.IStyleable
import krayon.editor.base.style.IStyleableContext
import krayon.editor.base.style.StyleProperty
import krayon.editor.sbgn.model.type
import java.awt.Color

class SbgnPolylineEdgeStyle(renderer:PolylineEdgeStyleRenderer = PolylineEdgeStyleRenderer()) : PolylineEdgeStyle(renderer), IStyleable {

    private var arrowScale = 1.0

    override fun retrieveStyle(context: IStyleableContext, map: MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.PathColor] = pen.paint as Color
        map[StyleProperty.PathWidth] = pen.thickness
        map[StyleProperty.ArrowScale] = arrowScale
        map[StyleProperty.PathCornerRadius] = smoothingLength
    }

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        map[StyleProperty.PathColor]?.let {
            pen = Pen(it as Color, pen.thickness)
            val type = (context.item as? IEdge)?.type
            if(type != null && targetArrow != null) {
                targetArrow = SbgnArrows.create(type, pen)
            }
        }
        map[StyleProperty.ArrowScale]?.let {
            val type = (context.item as? IEdge)?.type
            if(type != null && targetArrow != null) {
                arrowScale = it as? Double ?: arrowScale
                targetArrow = SbgnArrows.create(type, pen, scaleX = arrowScale, scaleY = arrowScale)
            }
        }
        map[StyleProperty.PathWidth]?.let {
            pen = Pen(pen.paint, it as Double)
            val type = (context.item as? IEdge)?.type
            if(type != null && targetArrow != null) {
                targetArrow = SbgnArrows.create(type, pen, scaleX = arrowScale, scaleY = arrowScale)
            }
        }
        (map[StyleProperty.PathCornerRadius] as? Double)?.let { smoothingLength = it }
    }

}
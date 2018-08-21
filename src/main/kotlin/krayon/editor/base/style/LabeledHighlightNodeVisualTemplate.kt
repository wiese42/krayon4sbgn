/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.view.Pen
import krayon.editor.base.util.toRectD
import java.awt.Color
import java.awt.Font
import java.awt.geom.RoundRectangle2D

class LabeledHighlightNodeVisualTemplate(highlightPen: Pen, text:String, textColor: Color): HighlightNodeVisualTemplate(highlightPen) {
    init {
        val font = Font("Dialog", Font.PLAIN, 12)
        val size = TextVisual.calculatePreferredSize(text, font)
        val shape = RoundRectangle2D.Double(0.0, 0.0, size.width + 10, size.height + 10, 10.0, 10.0)
        val shapeVisual = ShapeVisualWithBounds(shape, null, highlightPen.paint)
        val textVisual = TextVisual(text, font, textColor, shape.bounds2D.toRectD())
        tagVisual =  VisualGroupWithBounds().apply {
            add(shapeVisual)
            add(textVisual)
        }
    }
}
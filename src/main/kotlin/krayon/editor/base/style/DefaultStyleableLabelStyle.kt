/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.styles.DefaultLabelStyle
import com.yworks.yfiles.graph.styles.DefaultLabelStyleRenderer
import com.yworks.yfiles.view.Pen
import java.awt.Color

open class DefaultStyleableLabelStyle(renderer: DefaultLabelStyleRenderer = DefaultLabelStyleRenderer()) : DefaultLabelStyle(renderer), IStyleable {

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.FontSize] as? Double)?.let {
            font = font.deriveFont(it.toFloat())

            context.graph?.adjustLabelPreferredSize(context.item as ILabel)
        }
        (map[StyleProperty.FontStyle] as? FontStyleValue)?.let {
            font = font.deriveFont(it.code)

            context.graph?.adjustLabelPreferredSize(context.item as ILabel)
        }
        (map[StyleProperty.TextColor] as? Color)?.let {  textPaint = it }
        (map[StyleProperty.LabelOutlineColor] as? Color)?.let { backgroundPen = if(it.alpha == 0) null else Pen(it, backgroundPen?.thickness ?: 1.0) }
        (map[StyleProperty.LabelOutlineWidth] as? Double)?.let { backgroundPen = if(backgroundPen != null) Pen(backgroundPen.paint, it) else null }
        (map[StyleProperty.LabelBackgroundColor] as? Color)?.let { backgroundPaint = it }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.FontSize] = font.size2D.toDouble()
        map[StyleProperty.FontStyle] = FontStyleValue.values().first { it.code == font.style }
        map[StyleProperty.TextColor] = textPaint as Color
        map[StyleProperty.LabelOutlineColor] = backgroundPen?.paint ?: Color(0,0,0,0)
        map[StyleProperty.LabelOutlineWidth] = backgroundPen?.thickness ?: 1.0
        map[StyleProperty.LabelBackgroundColor] = backgroundPaint ?: Color(0,0,0,0)
    }

}
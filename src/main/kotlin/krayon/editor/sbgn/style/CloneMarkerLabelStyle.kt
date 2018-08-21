/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.styles.DefaultLabelStyle
import com.yworks.yfiles.graph.styles.DefaultLabelStyleRenderer
import krayon.editor.base.style.FontStyleValue
import krayon.editor.base.style.IStyleable
import krayon.editor.base.style.IStyleableContext
import krayon.editor.base.style.StyleProperty
import java.awt.Color

open class CloneMarkerLabelStyle(renderer: DefaultLabelStyleRenderer = DefaultLabelStyleRenderer()) : DefaultLabelStyle(renderer), IStyleable {

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.CloneMarkerFontSize] as? Double)?.let {
            font = font.deriveFont(it.toFloat())
            context.graph?.adjustLabelPreferredSize(context.item as ILabel)
        }
        (map[StyleProperty.CloneMarkerFontStyle] as? FontStyleValue)?.let {
            font = font.deriveFont(it.code)
            context.graph?.adjustLabelPreferredSize(context.item as ILabel)
        }
        (map[StyleProperty.CloneMarkerTextColor] as? Color)?.let {  textPaint = it }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.CloneMarkerFontSize] = font.size2D.toDouble()
        map[StyleProperty.CloneMarkerFontStyle] = FontStyleValue.values().first { it.code == font.style }
        map[StyleProperty.CloneMarkerTextColor] = textPaint as Color
    }

}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.view.GraphComponent
import com.yworks.yfiles.view.Pen
import krayon.editor.base.style.*
import krayon.editor.base.util.ApplicationSettings
import java.awt.Color

open class StyleableGraphComponent<T>(private val typeKey:T) : GraphComponent(), IStyleable {

    var graphStyle: GraphStyle<T>? = null
    var highlightPen = Pen(ApplicationSettings.DEFAULT_HIGHLIGHT_COLOR.value as? Color ?: Color.LIGHT_GRAY,3.0)

    //IStyleable implementation
    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.BackgroundColor] as? Color)?.let { background = it }
        (map[StyleProperty.HighlightColor] as? Color)?.let { highlightPen = Pen(it, highlightPen.thickness) }
    }

    override fun retrieveStyle(context: IStyleableContext, map: MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.BackgroundColor] = background
        map[StyleProperty.HighlightColor] = highlightPen.paint as Color
    }

    fun createStyleMap(): StyleAttributes {
        return mutableMapOf(
                StyleProperty.BackgroundColor to background,
                StyleProperty.HighlightColor to highlightPen.paint as Color)
    }

    fun applyStyle(graphStyle: GraphStyle<T>?) {
        this.graphStyle = graphStyle
        graphStyle?.styleTemplateMap?.get(typeKey)?.let { map ->
            (map[StyleProperty.BackgroundColor] as? Color)?.let { background = it }
            (map[StyleProperty.HighlightColor] as? Color)?.let { highlightPen = Pen(it, highlightPen.thickness) }
        }
    }
}
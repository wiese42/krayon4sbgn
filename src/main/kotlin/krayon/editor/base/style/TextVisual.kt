/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.IRectangle
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.view.IRenderContext
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

class TextVisual(var text:String, var font: Font, var textColor: Paint, var layout: IRectangle): IVisualWithBounds {
    override val bounds: RectD get() = layout.toRectD()

    var horizontalAlignment = HorizontalAlignment.CENTER
    enum class HorizontalAlignment {
        LEFT,
        RIGHT,
        CENTER
    }

    fun update(text:String, font:Font, fontColor: Paint, layout:IRectangle) {
        this.text = text
        this.font = font
        this.textColor = fontColor
        this.layout = layout
    }

    override fun paint(context: IRenderContext, g: Graphics2D) {
        if(text.isEmpty()) return

        val gfx = g.create() as Graphics2D
        val frc = FontRenderContext(font.transform, true, true)
        val textLayout = TextLayout(text, font, frc)
        val textBounds = textLayout.bounds
        gfx.paint = textColor
        gfx.font = font
        gfx.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)

        val xPos = when(horizontalAlignment) {
            HorizontalAlignment.CENTER -> layout.x + (layout.width - textBounds.width) * 0.5
            HorizontalAlignment.LEFT -> layout.x
            HorizontalAlignment.RIGHT -> layout.x + layout.width - textBounds.width
        }
        gfx.drawString(text, xPos.toFloat(), (layout.y + (layout.height + textLayout.ascent - textLayout.descent) * 0.5).toFloat())

        gfx.dispose()
    }

    companion object {
        fun calculatePreferredSize(text:String, font:Font): SizeD {
            return TextLayout(text,font,FontRenderContext(font.transform, true, true)).bounds.let { SizeD(it.width, it.height) }
        }
    }
}
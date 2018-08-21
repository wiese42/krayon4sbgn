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
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.styles.AbstractNodeStyle
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import com.yworks.yfiles.view.Pen
import com.yworks.yfiles.view.VisualGroup
import krayon.editor.base.style.*
import krayon.editor.base.ui.create
import krayon.editor.sbgn.model.orientation
import krayon.editor.sbgn.model.type
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import kotlin.math.min

abstract class InputOutputNodeStyle : AbstractNodeStyle(), IStyleable {

    var pen: Pen? = Pen.getBlack()
    var paint: Paint? = Color.WHITE
    var font: Font = Font("Dialog", Font.BOLD, 12)
    var fontColor: Color = Color.BLACK
    private var prevSize: SizeD? = null
    @Suppress("MemberVisibilityCanBePrivate")
    var hasDropShadow:Boolean = false
    var portPen: Pen = Pen.getBlack()
    private var isDirty = true

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.FontSize] as? Double)?.let { font = font.deriveFont(it.toFloat()) }
        (map[StyleProperty.FontStyle] as? FontStyleValue)?.let { font = font.deriveFont(it.code) }
        (map[StyleProperty.TextColor] as? Color)?.let {  fontColor = it }
        (map[StyleProperty.OutlineColor] as? Color)?.let { pen = if(it.alpha == 0) null else Pen(it, pen?.thickness ?: 1.0) }
        (map[StyleProperty.OutlineWidth] as? Double)?.let { pen = if(pen != null) Pen(pen?.paint ?: Color.BLACK, it) else null }
        (map[StyleProperty.Background] as? Color)?.let { paint = it }
        (map[StyleProperty.PortColor] as? Color)?.let { portPen = Pen(it, portPen.thickness) }
        (map[StyleProperty.PortWidth] as? Double)?.let { portPen = Pen(portPen.paint, it) }
        (map[StyleProperty.DropShadow] as? Boolean)?.let { hasDropShadow = it; isDirty = true }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.FontSize] = font.size2D.toDouble()
        map[StyleProperty.FontStyle] = FontStyleValue.values().first { it.code == font.style }
        map[StyleProperty.TextColor] = fontColor
        map[StyleProperty.OutlineColor] = pen?.paint ?: Color(0,0,0,0)
        map[StyleProperty.OutlineWidth] = pen?.thickness ?: 1.0
        map[StyleProperty.Background] = paint ?: Color(0,0,0,0)
        map[StyleProperty.PortColor] = portPen.paint
        map[StyleProperty.PortWidth] = portPen.thickness
        map[StyleProperty.DropShadow] = hasDropShadow
    }

    protected abstract fun getCoreShape(node:INode, size:SizeD):Shape
    protected abstract fun paintCore(context:IRenderContext, gfx: Graphics2D, node:INode, size: SizeD)

    inner class InputOutputVisual : IVisual {
        lateinit var node: INode
        fun update(node: INode) {
            this.node = node
        }

        override fun paint(context: IRenderContext, g: Graphics2D) {
            g.create { gfx ->
                val isHorizontal = node.orientation == "horizontal"
                with(node.layout) {
                    val delta = width - height
                    val coreSize = if (delta >= 0) SizeD(width - delta, height) else SizeD(width, height + delta)
                    if (delta != 0.0 && pen != null) {
                        portPen.adopt(gfx)
                        if (isHorizontal && delta > 0) {
                            val line = Line2D.Double(0.0, 0.5 * height, 0.5*delta, 0.5 * height)
                            gfx.draw(line)
                            line.setLine(width, 0.5 * height, width - 0.5*delta, 0.5 * height)
                            gfx.draw(line)
                        } else if (!isHorizontal && delta < 0) {
                            val line = Line2D.Double(0.5 * width, 0.0, 0.5 * width, -0.5*delta)
                            gfx.draw(line)
                            line.setLine(0.5 * width, height, 0.5 * width, height + 0.5*delta)
                            gfx.draw(line)
                        }
                    }
                    val coreOffset = getCoreOffset(node)
                    gfx.translate(coreOffset.x, coreOffset.y)
                    paintShape(gfx, getCoreShape(node, coreSize), pen, paint)
                    paintCore(context, gfx, node, coreSize)
                    gfx.translate(-coreOffset.x, -coreOffset.y)
                }
            }
        }
    }

    private fun getCoreOffset(node:INode):PointD {
        return with(node.layout) {
            if(width > height) PointD(0.5*(width-height), 0.0)
            else PointD(0.0, 0.5*(height-width))
        }
    }

    override fun createVisual(context: IRenderContext, node: INode): IVisual {
        val inputOutputVisual = InputOutputVisual()
        inputOutputVisual.update(node)
        return VisualGroup().apply {
            if(hasDropShadow) {
                val shadowGroup = VisualGroup().apply {
                    val dropShadow = DropShadowVisual(
                            DropShadowVisual.DropShadowParam(0, 4, 8, Color(0f, 0f, 0f, 0.2f)),
                            DropShadowVisual.DropShadowParam(0, 6, 20, Color(0f, 0f, 0f, 0.19f))
                    )
                    val coreDim = min(node.layout.width, node.layout.height)
                    val coreSize = SizeD(coreDim, coreDim)
                    val shadowShape:Shape = getCoreShape(node, coreSize)
                    dropShadow.update(coreSize, shadowShape, node.type)
                    add(dropShadow)
                    val coreOffset = getCoreOffset(node)
                    transform = AffineTransform.getTranslateInstance(coreOffset.x, coreOffset.y)
                }
                add(shadowGroup)
            }
            add(inputOutputVisual)
            transform = AffineTransform.getTranslateInstance(node.layout.x, node.layout.y)
        }
    }

    override fun updateVisual(context: IRenderContext, visual: IVisual, node: INode): IVisual {
        if(isDirty) {
            isDirty = false
            return createVisual(context, node)
        }
        val group = visual as VisualGroup
        val size = node.layout.toSizeD()

        if(hasDropShadow) {
            if(size != prevSize) {
                val shadowGroup = group.children[0] as VisualGroup
                val dropShadow = shadowGroup.children[0] as DropShadowVisual
                val coreDim = min(size.width, size.height)
                val coreSize = SizeD(coreDim, coreDim)
                val shadowShape:Shape = getCoreShape(node, SizeD(coreDim, coreDim))
                dropShadow.update(coreSize, shadowShape, node.type)
                val coreOffset = getCoreOffset(node)
                shadowGroup.transform = AffineTransform.getTranslateInstance(coreOffset.x, coreOffset.y)
            }
            (group.children[1] as InputOutputVisual).update(node)
        }
        else {
            (group.children[0] as InputOutputVisual).update(node)
        }

        prevSize = size
        group.transform = AffineTransform.getTranslateInstance(node.layout.x, node.layout.y)
        return group
    }

    protected fun paintShape(gfx:Graphics2D, shape:Shape, pen:Pen?, paint:Paint?) {
        if(paint != null) {
            gfx.paint = paint
            gfx.fill(shape)
        }
        if(pen != null) {
            pen.adopt(gfx)
            gfx.draw(shape)
        }
    }
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.*
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterFinder
import com.yworks.yfiles.graph.styles.DefaultLabelStyle
import com.yworks.yfiles.graph.styles.DefaultLabelStyleRenderer
import com.yworks.yfiles.graph.styles.ILabelStyle
import com.yworks.yfiles.graph.styles.INodeStyle
import com.yworks.yfiles.view.Pen
import com.yworks.yfiles.view.TextAlignment
import com.yworks.yfiles.view.VerticalAlignment
import krayon.editor.base.style.*
import krayon.editor.base.util.arcTo
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.type
import yfiles.patch.NodeStyleLabelStyleAdapter
import java.awt.Color
import java.awt.Font
import java.awt.Paint
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout

class SbgnAuxUnitLabelStyle(shapeType:AuxUnitShape, insets:InsetsD) : NodeStyleLabelStyleAdapter(AuxUnitShapeStyle().apply {  auxUnitShapeType = shapeType }, MyLabelStyle(insets)), IStyleable {

    var backgroundPen: Pen
        get() = (nodeStyle as AuxUnitShapeStyle).pen
        set(value) { (nodeStyle as AuxUnitShapeStyle).pen = value }

    var backgroundPaint: Paint
        get() = (nodeStyle as AuxUnitShapeStyle).paint
        set(value) { (nodeStyle as AuxUnitShapeStyle).paint = value }

    var font: Font
        get() = (labelStyle as DefaultLabelStyle).font
        set(value) {
            val frc = FontRenderContext(font.transform, true, true)
            val textLayout = TextLayout("X", font, frc)
            with(labelStyle as MyLabelStyle) {
                font = value
                minHeight = (textLayout.ascent + textLayout.descent).toDouble() + insets.verticalInsets
            }
        }

    var insets: InsetsD
        get() = (labelStyle as DefaultLabelStyle).insets
        set(value) {
            with(labelStyle as MyLabelStyle) {
                insets = value
                font = font //side-effect: update minHeight
            }
        }

    @Suppress("MemberVisibilityCanBePrivate")
    var textPaint: Paint?
        get() = (labelStyle as DefaultLabelStyle).textPaint
        set(value) { (labelStyle as DefaultLabelStyle).textPaint = value }

    enum class AuxUnitShape {
        RECTANGLE,
        ELLIPSE,
        CAPSULE
    }

    class AuxUnitShapeStyle : GeneralPathNodeStyle() {
        init {
            hasDropShadow = false
        }
        var auxUnitShapeType = AuxUnitShape.CAPSULE
        override fun createGeneralPath(node: INode, size: SizeD): GeneralPath {
            //println("size=$size")
            val path = GeneralPath()
            when (auxUnitShapeType) {
                AuxUnitShape.ELLIPSE -> path.appendEllipse(RectD(PointD.ORIGIN, size), false)
                AuxUnitShape.CAPSULE -> {
                    val width = size.width
                    val height = size.height
                    val radius = height / 2
                    return GeneralPath().apply {
                        moveTo(radius,0.0)
                        arcTo(radius, radius, radius, 1.5*Math.PI, 0.5*Math.PI)
                        lineTo(width-radius,height)
                        arcTo(radius, width-radius, radius, 2.5*Math.PI, 1.5*Math.PI)
                        close()
                    }
                }
                AuxUnitShape.RECTANGLE -> path.appendRectangle(RectD(PointD.ORIGIN, size), false)

            }
            return path
        }
    }

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.FontSize] as? Double)?.let {
            font = font.deriveFont(it.toFloat())
            adjustToPossibleSizeChange(context)
        }
        (map[StyleProperty.FontStyle] as? FontStyleValue)?.let {
            font = font.deriveFont(it.code)
            adjustToPossibleSizeChange(context)
        }
        (map[StyleProperty.TextColor] as? Color)?.let {  textPaint = it }
        (map[StyleProperty.LabelOutlineColor] as? Color)?.let { backgroundPen = Pen(it, backgroundPen.thickness) }
        (map[StyleProperty.LabelOutlineWidth] as? Double)?.let { backgroundPen = Pen(backgroundPen.paint, it) }
        (map[StyleProperty.LabelBackgroundColor] as? Color)?.let { backgroundPaint = it }
        (map[StyleProperty.LabelInsets] as? InsetsD)?.let {
            insets = it
            adjustToPossibleSizeChange(context)
        }

        if((context.item as? ILabel)?.type == SbgnType.STATE_VARIABLE) {
            (map[StyleProperty.StateVariableShape] as? StateVariableShapeValue)?.let {
                when(it) {
                    StateVariableShapeValue.Capsule -> (nodeStyle as AuxUnitShapeStyle).auxUnitShapeType = AuxUnitShape.CAPSULE
                    else -> (nodeStyle as AuxUnitShapeStyle).auxUnitShapeType = AuxUnitShape.ELLIPSE
                }
            }
        }
    }

    private fun adjustToPossibleSizeChange(context:IStyleableContext) {
        if(context.graph != null && context.item is ILabel) {
            val graph = context.graph!!
            val label = context.item as ILabel
            context.graph?.adjustLabelPreferredSize(context.item as ILabel)
            (label.layoutParameter.model as? ILabelModelParameterFinder)?.let {
                graph.setLabelLayoutParameter(label, it.findBestParameter(label, label.layoutParameter.model, label.layout))
            }
        }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.FontSize] = font.size2D.toDouble()
        map[StyleProperty.FontStyle] = FontStyleValue.values().first { it.code == font.style }
        map[StyleProperty.TextColor] = textPaint as Color
        map[StyleProperty.LabelOutlineColor] = backgroundPen.paint
        map[StyleProperty.LabelOutlineWidth] = backgroundPen.thickness
        map[StyleProperty.LabelBackgroundColor] = backgroundPaint
        map[StyleProperty.LabelInsets] = insets

        if((context.item as? ILabel)?.type == SbgnType.STATE_VARIABLE) {
            map[StyleProperty.StateVariableShape] = when((nodeStyle as AuxUnitShapeStyle).auxUnitShapeType) {
                    AuxUnitShape.CAPSULE -> StateVariableShapeValue.Capsule
                    else -> StateVariableShapeValue.Ellipse
            }
        }
    }

    class MyLabelStyle(insets: InsetsD) : DefaultStyleableLabelStyle(object:DefaultLabelStyleRenderer() {
        override fun getPreferredSize(): SizeD {
            return if(label.text.isEmpty()) {
                val size = super.getPreferredSize()
                val minHeight = (style as MyLabelStyle).minHeight
                when {
                    size.height < minHeight -> SizeD(minHeight, minHeight)
                    size.width < size.height -> SizeD(size.height, size.height)
                    else -> size
                }
            }
            else super.getPreferredSize()
        }
    }) {
        var minHeight:Double = 5.0
        init {
            this.insets = insets
            textAlignment = TextAlignment.CENTER
            verticalTextAlignment = VerticalAlignment.CENTER
            isTextClippingEnabled = false
        }
    }

    override fun clone(): NodeStyleLabelStyleAdapter {
        val copy = super.clone()
        copy.nodeStyle = nodeStyle.clone() as INodeStyle
        copy.labelStyle = labelStyle.clone() as ILabelStyle
        return copy
    }
}


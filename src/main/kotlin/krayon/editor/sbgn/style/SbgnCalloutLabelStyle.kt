/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.IPoint
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.SimpleNode
import com.yworks.yfiles.graph.styles.AbstractLabelStyle
import com.yworks.yfiles.view.*
import com.yworks.yfiles.view.input.*
import krayon.editor.base.style.*
import krayon.editor.base.util.addValueUndoEdit
import krayon.editor.base.util.convertFromRatioPoint
import krayon.editor.base.util.convertToRatioPoint
import krayon.editor.sbgn.model.SbgnPropertyKey
import krayon.editor.sbgn.model.getSbgnProperty
import krayon.editor.sbgn.model.setSbgnProperty
import java.awt.Color
import java.awt.Cursor
import java.awt.Paint

class SbgnCalloutLabelStyle(private val labelStyleDelegate: DefaultStyleableLabelStyle = DefaultStyleableLabelStyle()) : AbstractLabelStyle(), IHandleProvider, Cloneable, IStyleable {

    private val nodeStyle = AnnotationNodeStyle()

    private var label:ILabel? = null //required for node.layout
    private var graph: IGraph? = null

    var calloutPoint: PointD
        get() = label?.getSbgnProperty(SbgnPropertyKey.CALLOUT_POINT) as? PointD ?: PointD.ORIGIN
        set(value) { label?.setSbgnProperty(SbgnPropertyKey.CALLOUT_POINT, value) }


    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        with(labelStyleDelegate) {
            (map[StyleProperty.FontSize] as? Double)?.let {
                font = font.deriveFont(it.toFloat())
                context.graph?.adjustLabelPreferredSize(context.item as ILabel)

            }
            (map[StyleProperty.FontStyle] as? FontStyleValue)?.let {
                font = font.deriveFont(it.code)
                context.graph?.adjustLabelPreferredSize(context.item as ILabel)
            }
            (map[StyleProperty.TextColor] as? Color)?.let {  textPaint = it }
        }
        with(nodeStyle) {
            (map[StyleProperty.LabelOutlineColor] as? Color)?.let { pen = Pen(it, pen.thickness) }
            (map[StyleProperty.LabelOutlineWidth] as? Double)?.let { pen = Pen(pen.paint, it)}
            (map[StyleProperty.LabelBackgroundColor] as? Paint)?.let { paint = it }
            (map[StyleProperty.DropShadow] as? Boolean)?.let { hasDropShadow = it }
        }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.FontSize] = labelStyleDelegate.font.size2D.toDouble()
        map[StyleProperty.FontStyle] = FontStyleValue.values().first { it.code == labelStyleDelegate.font.style }
        map[StyleProperty.TextColor] = labelStyleDelegate.textPaint as Color
        map[StyleProperty.LabelOutlineColor] = nodeStyle.pen.paint ?: Color(0,0,0,0)
        map[StyleProperty.LabelOutlineWidth] = nodeStyle.pen.thickness
        map[StyleProperty.LabelBackgroundColor] = nodeStyle.paint
        map[StyleProperty.DropShadow] = nodeStyle.hasDropShadow
    }

    override fun getPreferredSize(label: ILabel): SizeD {
        this.label = label
        if(label.preferredSize != null) return label.preferredSize
        val size = labelStyleDelegate.renderer.getPreferredSize(label, labelStyleDelegate)
        val corner = nodeStyle.calcCornerSize(size)
        return SizeD(size.width+corner*2.0, size.height+corner*2.0)
    }

    override fun createVisual(context: IRenderContext, label: ILabel): IVisual {
        this.label = label
        this.graph = (context.canvasComponent as GraphComponent).graph

        val node = SimpleNode()
        node.tag = "__SbgnCalloutLabelStyle"
        node.layout = updateAnnotationShape(context, label)
        val group = VisualGroup()
        val shapeVisual = nodeStyle.renderer.getVisualCreator(node, nodeStyle).createVisual(context)
        val labelVisual = labelStyleDelegate.renderer.getVisualCreator(label, labelStyleDelegate).createVisual(context)
        group.add(shapeVisual)
        group.add(labelVisual)
        return group
    }

    private fun updateAnnotationShape(context: ICanvasContext, label: ILabel): RectD {
        if (label.owner is INode) {
            val ownerBox = (label.owner as INode).layout
            val tipPoint = ownerBox.convertFromRatioPoint(calloutPoint)
            //println("calloutPoint=$calloutPoint  tipPoint=$tipPoint")
            val labelBox = labelStyleDelegate.renderer.getBoundsProvider(label, labelStyleDelegate).getBounds(context)
            val shapeBox = RectD.add(labelBox, tipPoint)
            //println("labelBox=$labelBox   shapeBox=$shapeBox")
            nodeStyle.tipAnchor = shapeBox.convertToRatioPoint(tipPoint)
            nodeStyle.topLeftBoxAnchor = shapeBox.convertToRatioPoint(labelBox.topLeft)
            nodeStyle.bottomRightBoxAnchor = shapeBox.convertToRatioPoint(labelBox.bottomRight)
            return shapeBox
        }
        return RectD.EMPTY
    }

    override fun getHandles(context: IInputModeContext?): MutableIterable<IHandle> {
        return mutableListOf(TipHandle()) //, SizeHandle())
    }

    override fun lookup(label: ILabel, type: Class<*>): Any? {
        return when(type) {
            IReshapeHandleProvider::class.java -> {
                LabelReshapeHandleProvider(label, HandlePositions.BORDER)
            }
            else -> super.lookup(label, type)
        }
    }

    inner class TipHandle: IHandle {
        override fun handleMove(context: IInputModeContext?, origP: PointD, newP: PointD) {
            val nodeLayout = (label?.owner as INode).layout
            calloutPoint = nodeLayout.convertToRatioPoint(newP)
        }
        override fun getLocation(): IPoint {
            val nodeLayout = (label?.owner as INode).layout
            return nodeLayout.convertFromRatioPoint(calloutPoint)
        }
        override fun initializeDrag(p0: IInputModeContext) {}

        override fun dragFinished(context: IInputModeContext, p1: PointD, p2: PointD) {
            val nodeLayout = (label?.owner as INode).layout
            calloutPoint = nodeLayout.convertToRatioPoint(p2)
            context.graph.addValueUndoEdit("Move Callout Tip Position",
                    nodeLayout.convertToRatioPoint(p1), calloutPoint, { calloutPoint = it })
        }
        override fun cancelDrag(p0: IInputModeContext?, p1: PointD?) {}
        override fun getType() = HandleTypes.MOVE!!
        override fun getCursor() = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)!!
    }

    override fun clone(): SbgnCalloutLabelStyle {
        return super<AbstractLabelStyle>.clone() as SbgnCalloutLabelStyle
    }
}

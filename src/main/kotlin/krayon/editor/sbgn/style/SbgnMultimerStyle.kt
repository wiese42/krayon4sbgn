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
import com.yworks.yfiles.graph.IModelItem
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.styles.AbstractNodeStyle
import com.yworks.yfiles.utils.IEventListener
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import com.yworks.yfiles.view.Pen
import com.yworks.yfiles.view.VisualGroup
import com.yworks.yfiles.view.input.GraphEditorInputMode
import com.yworks.yfiles.view.input.IInputModeContext
import com.yworks.yfiles.view.input.ItemClickedEventArgs
import krayon.editor.base.style.DropShadowVisual
import krayon.editor.base.style.IStyleable
import krayon.editor.base.style.IStyleableContext
import krayon.editor.base.style.StyleProperty
import krayon.editor.base.ui.create
import krayon.editor.sbgn.model.*
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Rectangle2D

abstract class SbgnMultimerStyle : AbstractNodeStyle(), IStyleable {

    var pen: Pen = Pen.getBlack()
    var paint: Paint = Color.WHITE
    var shapeParameter: Double = 10.0
    @Suppress("MemberVisibilityCanBePrivate")
    var multimerOffset:Double = 5.0
    @Suppress("MemberVisibilityCanBePrivate")
    var clonePercentage: Double = 0.25
    @Suppress("MemberVisibilityCanBePrivate")
    var clonePaint: Paint? = Color.GRAY
    @Suppress("MemberVisibilityCanBePrivate")
    var hasDropShadow = false

    private var prevSize: SizeD? = null
    private var isDirty:Boolean = true

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.OutlineColor] as? Color)?.let { pen = Pen(it, pen.thickness) }
        (map[StyleProperty.OutlineWidth] as? Double)?.let { pen = Pen(pen.paint, it) }
        (map[StyleProperty.Background] as? Paint)?.let { paint = it }
        (map[StyleProperty.DropShadow] as? Boolean)?.let { hasDropShadow = it; isDirty = true }
        (map[StyleProperty.ShapeParameter] as? Double)?.let { shapeParameter = it }
        SbgnBuilder.applyStyleToNameLabel(context, map)
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.OutlineColor] = pen.paint as Color
        map[StyleProperty.OutlineWidth] = pen.thickness
        map[StyleProperty.Background] = paint
        map[StyleProperty.DropShadow] = hasDropShadow
        map[StyleProperty.ShapeParameter] = shapeParameter
        SbgnBuilder.retrieveStyleFromNameLabel(context, map)
    }

    override fun getOutline(node: INode): com.yworks.yfiles.geometry.GeneralPath {
        if(node.getSbgnProperty(SbgnPropertyKey.IS_FROZEN) as? Boolean == true) return GeneralPath()

        val outline = GeneralPath()
        outline.appendRectangle(node.layout, true)
        return outline
    }

    private fun isFrozen(context: IInputModeContext, node:INode):Boolean {
        if(context.graph.contains(node)) {
            val gs = context.graph.groupingSupport
            gs.getPathToRoot(node).drop(1).forEach {
                if (it.getSbgnProperty(SbgnPropertyKey.IS_LOCKED) as? Boolean == true) return true
            }
        }
        return false
    }

    override fun isHit(context: IInputModeContext, p: PointD, node: INode): Boolean {
        return if(isFrozen(context, node)) false else super.isHit(context, p, node)
    }

    override fun isInBox(context: IInputModeContext, box: RectD, node: INode): Boolean {
        return if(isFrozen(context, node)) false else super.isInBox(context, box, node)
    }

    protected abstract fun createShape(node:INode, size:SizeD):GeneralPath

    fun paintPath(gfx:Graphics2D, node:INode, path:GeneralPath) {
        val path2D = path.createPath(Matrix2D())
        gfx.paint = paint

        gfx.fill(path2D)
        if(node.isClone && clonePaint != null) {
            val cloneGfx = gfx.create() as Graphics2D
            val bounds = path.bounds
            cloneGfx.clip(Rectangle2D.Double(0.0, bounds.height*(1.0-clonePercentage), bounds.width, bounds.height*clonePercentage))
            cloneGfx.paint = clonePaint
            cloneGfx.fill(path2D)
            cloneGfx.dispose()
        }
        pen.adopt(gfx)
        gfx.draw(path2D)
    }

    inner class MultimerVisual : IVisual {
        lateinit var node:INode
        lateinit var shape:GeneralPath

        fun update(node:INode) {
            this.node = node
            shape = if(node.type.isMultimer()) {
                createShape(node, SizeD(node.layout.width-multimerOffset,node.layout.height-multimerOffset))
            } else {
                createShape(node, node.layout.toSizeD())
            }
        }

        override fun paint(context: IRenderContext, g: Graphics2D) {
            g.create { gfx ->
                if(node.type.isMultimer()) {
                    gfx.translate(multimerOffset, multimerOffset)
                    paintPath(gfx, node, shape)
                    gfx.translate(-multimerOffset, -multimerOffset)
                    paintPath(gfx, node, shape)
                }
                else {
                    val shape = createShape(node, node.layout.toSizeD())
                    paintPath(gfx, node, shape)
                }
            }
        }
    }

    override fun createVisual(context: IRenderContext, node: INode): IVisual {
        val multimerVisual = MultimerVisual()
        multimerVisual.update(node)
        return VisualGroup().apply {

            if(hasDropShadow) {
                val dropShadow = DropShadowVisual(
                        DropShadowVisual.DropShadowParam(0, 4, 8, Color(0f, 0f, 0f, 0.2f)),
                        DropShadowVisual.DropShadowParam(0, 6, 20, Color(0f, 0f, 0f, 0.19f))
                )
                //val dropShadow = DropShadowVisual.createTestDropShadow()
                val size = node.layout.toSizeD()
                var shadowShape:Shape = multimerVisual.shape.createPath(Matrix2D())
                if(node.type.isMultimer()) {
                    val shadowArea = Area(shadowShape)
                    shadowArea.add(shadowArea.createTransformedArea(AffineTransform.getTranslateInstance(multimerOffset, multimerOffset)))
                    shadowShape = shadowArea
                }
                dropShadow.update(size, shadowShape, node.type)
                add(dropShadow)
            }
            add(multimerVisual)
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
        if(size != prevSize) {
            //println("updating! size changed")
            prevSize = size
            if(hasDropShadow) {
                val multimerVisual = group.children[1] as MultimerVisual
                multimerVisual.update(node)
                var shadowShape:Shape = multimerVisual.shape.createPath(Matrix2D())
                if(node.type.isMultimer()) {
                    val shadowArea = Area(shadowShape)
                    shadowArea.add(shadowArea.createTransformedArea(AffineTransform.getTranslateInstance(multimerOffset, multimerOffset)))
                    shadowShape = shadowArea
                }
                (group.children[0] as DropShadowVisual).update(size, shadowShape, node.type)
            }
            else {
                (group.children[0] as MultimerVisual).update(node)
            }
        }
        group.transform = AffineTransform.getTranslateInstance(node.layout.x, node.layout.y)
        return group
    }

    private fun isInCloneMarkerRegion(node:INode, p:IPoint) = p.y > node.layout.y + node.layout.height * (1.0-clonePercentage)

    companion object {
        fun addCloneMarkerLabelOnClickListener(geim: GraphEditorInputMode): IEventListener<ItemClickedEventArgs<IModelItem>> {
            return IEventListener { _, args ->
                val node = args.item
                if(node is INode && node.isClone && node.labels.none { it.type == SbgnType.CLONE_LABEL } &&
                        (node.style as? SbgnMultimerStyle)?.isInCloneMarkerRegion(node, args.location) == true) {
                    args.isHandled = true
                    val label = geim.graph.addLabel(node, "marker")
                    label.type = SbgnType.CLONE_LABEL
                    SbgnBuilder.configure(geim.graph, label)
                    SbgnBuilder.applyStyle(geim.graph, label.owner)
                    geim.editLabel(label)
                }
            }
        }
   }
}

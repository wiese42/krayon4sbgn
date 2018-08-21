/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.GeneralPath
import com.yworks.yfiles.geometry.Matrix2D
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.styles.AbstractNodeStyle
import com.yworks.yfiles.view.*
import krayon.editor.base.util.update
import java.awt.Color
import java.awt.Paint
import java.awt.geom.AffineTransform

abstract class GeneralPathNodeStyle: AbstractNodeStyle(), IStyleable, Cloneable {

    var pen: Pen = Pen.getBlack()
    set(value) { field = value; requiresVisualUpdate = true}
    var paint: Paint = Color.WHITE
    set(value) { field = value; requiresVisualUpdate = true}
    var hasDropShadow = false
    private var isDirty = false

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.OutlineColor] as? Color)?.let { pen = Pen(it, pen.thickness) }
        (map[StyleProperty.OutlineWidth] as? Double)?.let { pen = Pen(pen.paint, it) }
        (map[StyleProperty.Background] as? Paint)?.let { paint = it }
        (map[StyleProperty.DropShadow] as? Boolean)?.let { hasDropShadow = it; isDirty = true }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.OutlineColor] = pen.paint ?: Color(0,0,0,0)
        map[StyleProperty.OutlineWidth] = pen.thickness
        map[StyleProperty.Background] = paint
        map[StyleProperty.DropShadow] = hasDropShadow
    }

    protected var requiresVisualUpdate:Boolean = true
    private var outline: GeneralPath? = null
    protected var shape: java.awt.geom.Path2D? = null

    private var prevSize: SizeD? = null

    abstract fun createGeneralPath(node:INode, size: SizeD): GeneralPath

    override fun createVisual(context: IRenderContext?, node: INode) = privateCreateVisual(node)

    private fun privateCreateVisual(node: INode): IVisual {
        //println("createVisual")
        val visual = ShapeVisual()
        outline = createGeneralPath(node, node.layout.toSizeD())
        shape = outline!!.createPath(Matrix2D())
        visual.update(shape!!, pen, paint)

        if(hasDropShadow) {
            val dropShadow = DropShadowVisual(
                    DropShadowVisual.DropShadowParam(0, 4, 8, Color(0f, 0f, 0f, 0.2f)),
                    DropShadowVisual.DropShadowParam(0, 6, 20, Color(0f, 0f, 0f, 0.19f))
            )
            dropShadow.update(node.layout.toSizeD(), shape!!, node.tag) //breaks base <--> sbgn packaging
            return VisualGroup().apply {
                add(dropShadow)
                add(visual)
                transform = AffineTransform.getTranslateInstance(node.layout.x, node.layout.y)
            }
        }
        else {
            return VisualGroup().apply {
                add(visual)
                transform = AffineTransform.getTranslateInstance(node.layout.x, node.layout.y)
            }
        }
    }

    override fun updateVisual(context: IRenderContext, visual: IVisual, node: INode): IVisual {
        //println("updateVisual")
        if(isDirty) {
            isDirty = false
            return privateCreateVisual(node)
        }

        val size = node.layout.toSizeD()
        if(size != prevSize || requiresVisualUpdate) {
            outline = createGeneralPath(node, size)
            shape = outline!!.createPath(Matrix2D())
            prevSize = size
            requiresVisualUpdate = false
        }
        val group = (visual as VisualGroup)
        if(hasDropShadow) {
            (group.children[0] as DropShadowVisual).update(node.layout.toSizeD(), shape!!, node.tag)
            (group.children[1] as ShapeVisual).update(shape!!, pen, paint)
        }
        else {
            (group.children[0] as ShapeVisual).update(shape!!, pen, paint)
        }
        group.transform = AffineTransform.getTranslateInstance(node.layout.x, node.layout.y)
        return group
    }

    override fun getOutline(node: INode): GeneralPath {
        return if(outline == null)
            GeneralPath().apply { appendRectangle(node.layout, false) }
        else {
            val matrix = Matrix2D().apply { translate(node.layout.toPointD()) }
            outline!!.createGeneralPath(matrix)
        }
    }

    override fun clone(): AbstractNodeStyle {
        return super<AbstractNodeStyle>.clone()
    }
}

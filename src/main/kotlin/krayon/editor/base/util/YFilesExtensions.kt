/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.util

import com.yworks.yfiles.geometry.*
import com.yworks.yfiles.graph.GraphItemTypes
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameter
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterFinder
import com.yworks.yfiles.view.*
import com.yworks.yfiles.view.input.GraphEditorInputMode
import com.yworks.yfiles.view.input.IEventRecognizer
import com.yworks.yfiles.view.input.IInputModeContext
import krayon.editor.base.style.ValueUndoUnit
import java.awt.Paint
import java.awt.Shape
import java.awt.geom.Rectangle2D
import kotlin.math.max
import kotlin.math.min

fun GeneralPath.arcTo(r:Double, cx: Double, cy: Double, fromAngle: Double, toAngle: Double) {
    val a = (toAngle - fromAngle) / 2.0
    val sgn = if (a < 0) -1 else 1
    if (Math.abs(a) > Math.PI / 4) {
        // bigger then a quarter circle -> split into multiple arcs
        var start = fromAngle
        var end = fromAngle + sgn * Math.PI / 2
        while (sgn * end < sgn * toAngle) {
            arcTo(r, cx, cy, start, end)
            start = end
            end += sgn * Math.PI / 2
        }
        arcTo(r, cx, cy, start, toAngle)
        return
    }

    // calculate unrotated control points
    val x1 = r * Math.cos(a)
    val y1 = -r * Math.sin(a)

    val m = (Math.sqrt(2.0) - 1) * 4 / 3
    val mTanA = m * Math.tan(a)

    val x2 = x1 - mTanA * y1
    val y2 = y1 + mTanA * x1
    val y3 = -y2

    // rotate the control points by (fromAngle + a)
    val rot = fromAngle + a
    val sinRot = Math.sin(rot)
    val cosRot = Math.cos(rot)
    cubicTo(cx + x2 * cosRot - y2 * sinRot, cy + x2 * sinRot + y2 * cosRot, cx + x2 * cosRot - y3 * sinRot, cy + x2 * sinRot + y3 * cosRot, cx + r * Math.cos(toAngle), cy + r * Math.sin(toAngle))
}

fun IGraph.translate(offset: PointD) {
    nodes.forEach { setNodeCenter(it, PointD.add(it.layout.center, offset)) }
    bends.forEach { setBendLocation(it, PointD.add(it.location.toPointD(), offset)) }
}

fun IGraph.translate(offset: PointD, subgraphNodes:Iterable<INode>) {
    val nodeSet = subgraphNodes.toHashSet()
    subgraphNodes.forEach { setNodeCenter(it, PointD.add(it.layout.center, offset)) }
    bends.filter{ nodeSet.contains(it.owner.sourceNode) &&  nodeSet.contains(it.owner.targetNode) }.forEach { setBendLocation(it, PointD.add(it.location.toPointD(), offset)) }
}
fun IGraph.isAncestor(node: INode, maybeAncestor:INode?):Boolean {
    var parent = getParent(node)
    while (parent != null) {
        if(parent == maybeAncestor) return true
        else parent = getParent(parent)
    }
    return false
}

fun IGraph.getAncestors(node: INode):List<INode> {
    val result = mutableListOf<INode>()
    var parent = getParent(node)
    while (parent != null) {
        result.add(parent)
        parent = getParent(parent)
    }
    return result
}

fun IGraph.getBounds(): RectD = getBounds({ true })
fun IGraph.getBounds(isIncluded: NodePredicate, includeBends:Boolean = true, includeLabels:Boolean = true): RectD {
    val bounds = MutableRectangle()
    nodes.forEach{
        if(isIncluded(it)) {
            bounds.add(it.layout)
            if(includeLabels) it.labels.forEach { label ->
                bounds.add(label.bounds)
            }
        }
    }
    if(includeBends) {
        edges.forEach {
            if(it.bends.any() && isIncluded(it.sourceNode) && isIncluded(it.targetNode)) {
                it.bends.forEach { bend ->
                    bounds.add(bend.location)
                }
            }
        }
    }
    return bounds.toRectD()
}

fun IGraph.beginEdit(name:String = "") = this.beginEdit(name, name)!!
fun <T> IGraph.beginEdit(item:T) = this.beginEdit("","", listOf(item))!!
fun <T> IGraph.beginEdit(name:String, item:T) = this.beginEdit(name,name, listOf(item))!!
fun <T> IGraph.beginEdit(items: Iterable<T>) = beginEdit("","", items)!!
fun <T> IGraph.beginEdit(name:String, items: Iterable<T>) = beginEdit(name,name, items)!!
fun <T> IGraph.addValueUndoEdit(name:String, startValue:T, endValue:T, actor: (T) -> Unit) = undoEngine.addUnit(ValueUndoUnit(name, startValue, endValue, actor))

fun MutableRectangle.setBounds(x:Double, y:Double, width:Double, height:Double) {
    this.width = width
    this.height = height
    this.x = x
    this.y = y
}
fun MutableRectangle.enlargeTLRB(t:Double, l:Double, r:Double, b:Double) {
    width += l + r
    height += t + b
    x -= l
    y -= t
}

fun SizeD.withHeight(newHeight:Double) = SizeD(width, newHeight)
fun SizeD.withWidth(newWidth:Double) = SizeD(newWidth, height)
fun SizeD.scale(sx:Double, sy:Double) = SizeD(width*sx, height*sy)
fun SizeD.swap() = SizeD(height, width)

fun IRectangle.withWidth(newWidth:Double) = RectD(x,y,newWidth, height)
fun IRectangle.withHeight(newHeight:Double) = RectD(x,y,width, newHeight)
fun IRectangle.translate(delta: PointD) = RectD(x+delta.x, y+delta.y, width, height)
fun IRectangle.translate(deltaX: Double, deltaY:Double) = RectD(x+deltaX, y+deltaY, width, height)
operator fun IRectangle.minus(p:PointD) = RectD(x-p.x, y-p.y, width, height)
fun IRectangle.convertToRatioPoint(p:PointD) = PointD((p.x - x) / width, (p.y - y) / height)
fun IRectangle.convertFromRatioPoint(p:PointD) = PointD(x + (width*p.x), y + (height*p.y))
fun IRectangle.containsRect(rect:IRectangle) = contains(rect.bottomLeft) && contains(rect.bottomRight) && contains(topLeft) && contains(topRight)

fun IRectangle.toRectangle2D() = Rectangle2D.Double(x,y,width,height)
val IRectangle.centerLeft get() = PointD(x, y+0.5*height)
val IRectangle.centerRight get() = PointD(maxX, y+0.5*height)
val IRectangle.centerTop get() = PointD(x+0.5*width, y)
val IRectangle.centerBottom get() = PointD(x+0.5*width, maxY)

fun IPoint.distanceToSqr(p:IPoint):Double = (x-p.x)*(x-p.x)+(y-p.y)*(y-p.y)
fun IPoint.withX(newX:Double) = PointD(newX, y)
fun IPoint.withY(newY:Double) = PointD(x, newY)
operator fun IPoint.minus(p:PointD) = PointD(x-p.x,y-p.y)
operator fun IPoint.plus(p:PointD) = PointD(x+p.x,y+p.y)
operator fun IPoint.times(scalar:Double) = PointD(x*scalar,y*scalar)
fun IPoint.isInBottomOuterSector(box:IRectangle):Boolean {
    return y > box.maxY && ((x >= box.x && x <= box.maxX) || (x < box.x && box.x-x <= y-box.maxY) || (x > box.maxX && x-box.maxX <= y-box.maxY))
}
fun IPoint.isInTopOuterSector(box:IRectangle):Boolean {
    return y < box.y && ((x >= box.x && x <= box.maxX) || (x < box.x && box.x-x <= box.y-y) || (x > box.maxX && x-box.maxX <= box.y-y))
}
fun IPoint.isInLeftOuterSector(box:IRectangle):Boolean {
    return x < box.x && ((y >= box.y && y <= box.maxY) || (y < box.y && box.y-y <= box.x-x) || (y > box.maxY && y-box.maxY <= box.x-x))
}
fun IPoint.isInRightOuterSector(box:IRectangle):Boolean {
    return x > box.maxX && ((y >= box.y && y <= box.maxY) || (y < box.y && box.y-y <= x-box.maxX) || (y > box.maxY && y-box.maxY <= x-box.maxX))
}
fun IPoint.isOnBorder(box:IRectangle):Boolean = (x == box.x || x == box.maxX) && y >= box.y && y <= box.maxY || (y == box.y || y == box.maxY) && x >= box.x && x <= box.maxX
fun IPoint.clamp(min:IPoint, max:IPoint) = PointD(min(max(x,min.x),max.x), min(max(y,min.y),max.y))

operator fun PointD.unaryMinus() = PointD(-x,-y)


fun Rectangle2D.toRectD() = RectD(x,y,width,height)

fun Array<GraphItemTypes>.ensureBefore(item1:GraphItemTypes, item2:GraphItemTypes): Array<GraphItemTypes> {
    return this.toMutableList().apply {
        val index1 = indexOf(item1)
        val index2 = indexOf(item2)
        if(index1 >= 0 && index2 >= 0 && index1 > index2) {
            removeAt(index1)
            add(index2, item1)
        }
    }.toTypedArray()
}

fun ShapeVisual.update(shape: Shape, pen: Pen?, fill: Paint?) {
    this.shape = shape
    this.pen = pen
    this.fill = fill
}

val ILabel.bounds: RectD get() = layoutParameter.model.getGeometry(this,layoutParameter).bounds

fun ILabelModelParameterFinder.findBestParameter(label: ILabel, center:PointD):ILabelModelParameter {
    return findBestParameter(label, label.layoutParameter.model, OrientedRectangle(RectD.fromCenter(center,label.preferredSize)))
}

fun ILabelModelParameterFinder.findBestParameter(label: ILabel, box:RectD):ILabelModelParameter {
    return findBestParameter(label, label.layoutParameter.model, OrientedRectangle(box))
}


val MOUSE_MIDDLE_PRESSED = IEventRecognizer { _, args ->
    args is Mouse2DEventArgs && args.eventType == Mouse2DEventTypes.PRESSED && args.changedButtons == MouseButtons.MIDDLE
}

val GraphComponent.geim: GraphEditorInputMode
    get() = inputMode as GraphEditorInputMode

val IInputModeContext.graphComponent
    get() = canvasComponent as GraphComponent

val INode.center: PointD get() = layout.center
typealias NodePredicate = (INode) -> Boolean

fun GraphModelManager.getMainCanvasObject(node:INode):ICanvasObject {
    return if(graph.isGroupNode(node)) {
        val parent = graph.getParent(node)
        // determine the canvas object containing the visualization of the group node
        val groupNodeCO = getCanvasObject(node)
        // when group node already had children, the grand-parent group is the main canvas object group of group node
        // otherwise it is the main canvas object group of its parent node resp. the ContentGroup if the group node is top-level
        val groupGroup = groupNodeCO.group.group
        // determine the main canvas object group of the parent resp. the content group
        val useGroupNodeCO = if(parent == null) nodeGroup === groupGroup.group
        else getCanvasObject(parent).group.group == groupGroup

        if(useGroupNodeCO) groupNodeCO else groupGroup
    } else getCanvasObject(node)
}

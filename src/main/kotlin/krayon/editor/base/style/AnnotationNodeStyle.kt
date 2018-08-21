/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.*
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.FreeNodeLabelModel
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameter
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import com.yworks.yfiles.view.VisualGroup
import com.yworks.yfiles.view.input.HandleTypes
import com.yworks.yfiles.view.input.IHandle
import com.yworks.yfiles.view.input.IHandleProvider
import com.yworks.yfiles.view.input.IInputModeContext
import krayon.editor.base.util.*
import krayon.editor.sbgn.style.SbgnShapeNodeStyle
import java.awt.Cursor
import java.awt.Graphics2D

class AnnotationNodeStyle : SbgnShapeNodeStyle() {

    var topLeftBoxAnchor:PointD = PointD.ORIGIN!!
    var bottomRightBoxAnchor:PointD = PointD(1.0, 0.5)
    var tipAnchor:PointD = PointD(0.66, 1.0)
    private var dogEarSize = 10.0
    var showTipHandle = true

    override fun createGeneralPath(node:INode, size: SizeD): GeneralPath {

        val box = RectD(size.width*topLeftBoxAnchor.x, size.height*topLeftBoxAnchor.y, size.width*(bottomRightBoxAnchor.x-topLeftBoxAnchor.x), size.height*(bottomRightBoxAnchor.y-topLeftBoxAnchor.y))
        val tipPoint = PointD(size.width*tipAnchor.x, size.height*tipAnchor.y)
        val corner = calcCornerSize(size)
        //println("tipPoint=$tipPoint   box=$box")
        return GeneralPath().apply {
            moveTo(box.maxX-corner, box.y)
            lineTo(box.maxX, box.y + corner)
            when {
                tipPoint.isInBottomOuterSector(box) -> {
                    //tip to the bottom
                    val gap = Math.min(box.width*0.1,15.0)
                    lineTo(box.bottomRight)
                    lineTo(box.x+box.width*0.3+gap, box.maxY)
                    lineTo(tipPoint)
                    lineTo(box.x+box.width*0.3-gap, box.maxY)
                    lineTo(box.bottomLeft)
                    lineTo(box.topLeft)
                }
                tipPoint.isInLeftOuterSector(box) -> {
                    //tip to the left
                    val gap = Math.min(box.height*0.1,15.0)
                    lineTo(box.bottomRight)
                    lineTo(box.bottomLeft)
                    lineTo(box.x, box.maxY-box.height*0.3+gap)
                    lineTo(tipPoint)
                    lineTo(box.x, box.maxY-box.height*0.3-gap)
                    lineTo(box.topLeft)
                }
                tipPoint.isInRightOuterSector(box) -> {
                    //tip to the right
                    val gap = Math.min(box.height*0.1,15.0)
                    lineTo(box.maxX, box.maxY-box.height*0.3-gap)
                    lineTo(tipPoint)
                    lineTo(box.maxX, box.maxY-box.height*0.3+gap)
                    lineTo(box.bottomRight)
                    lineTo(box.bottomLeft)
                    lineTo(box.topLeft)
                }
                tipPoint.isInTopOuterSector(box) -> {
                    //tip to the top
                    val gap = Math.min(box.width*0.1,15.0)
                    lineTo(box.bottomRight)
                    lineTo(box.bottomLeft)
                    lineTo(box.topLeft)
                    lineTo(box.x+box.width*0.3-gap, box.y)
                    lineTo(tipPoint)
                    lineTo(box.x+box.width*0.3+gap, box.y)
                }
                else -> {
                    //no tip
                    lineTo(box.bottomRight)
                    lineTo(box.bottomLeft)
                    lineTo(box.topLeft)
                }
            }
            close()
        }
    }

    fun getBoxLayout(node:INode):RectD {
        return RectD(node.layout.convertFromRatioPoint(topLeftBoxAnchor), node.layout.convertFromRatioPoint(bottomRightBoxAnchor))
    }

    fun calcCornerSize(size:SizeD) = Math.min(size.width*0.20, dogEarSize)

    fun createLabelParamater(node:INode):ILabelModelParameter {
        val box = getBoxLayout(node)
        return FreeNodeLabelModel.INSTANCE.createParameter(node.layout.convertToRatioPoint(box.center), PointD.ORIGIN, PointD(0.5,0.5), PointD.ORIGIN, 0.0)
    }

    private inner class CornerVisual : IVisual {
        lateinit var cornerPath:java.awt.geom.GeneralPath

        fun update(layout:IRectangle) {
            val cornerSize = calcCornerSize(layout.toSizeD())
            val corner = PointD(layout.x+layout.width*bottomRightBoxAnchor.x, layout.y+layout.height*topLeftBoxAnchor.y )
            cornerPath = java.awt.geom.GeneralPath().apply {
                moveTo(corner.x-cornerSize, corner.y)
                lineTo(corner.x-cornerSize, corner.y+cornerSize)
                lineTo(corner.x, corner.y+cornerSize)
                closePath()
            }
        }
        override fun paint(context: IRenderContext, gfx: Graphics2D) {
            gfx.paint = pen.paint
            gfx.fill(cornerPath)
        }
    }

    override fun createVisual(context: IRenderContext?, node: INode): IVisual {
        return VisualGroup().apply {
            add(super.createVisual(context, node))
            add(CornerVisual().apply { update(node.layout)})
        }
    }

    override fun updateVisual(context: IRenderContext, visual: IVisual, node: INode): IVisual {
        val group = visual as VisualGroup
        group.children[0] = super.updateVisual(context, group.children[0], node)
        (group.children[1] as CornerVisual).update(node.layout)
        return  group
    }

    override fun lookup(node: INode, type: Class<*>): Any? {

        return when(type) {
            IHandleProvider::class.java -> {
                if(!showTipHandle) return super.lookup(node, type)
                else return IHandleProvider { _ ->
                    val tipHandle = object : IHandle {
                        override fun handleMove(context: IInputModeContext?, origP: PointD, newP: PointD) {
                            tipAnchor = node.layout.convertToRatioPoint(newP)
                        }

                        override fun getLocation(): IPoint {
                            return node.layout.convertFromRatioPoint(tipAnchor)
                        }

                        override fun initializeDrag(p0: IInputModeContext?) {}
                        override fun dragFinished(context: IInputModeContext, origPoint: PointD, newPoint: PointD) {
                            val box = getBoxLayout(node)
                            val newBounds = RectD.add(box, newPoint)
                            topLeftBoxAnchor = newBounds.convertToRatioPoint(box.topLeft)
                            bottomRightBoxAnchor = newBounds.convertToRatioPoint(box.bottomRight)
                            tipAnchor = newBounds.convertToRatioPoint(newPoint)
                            context.graph.apply {
                                setNodeLayout(node, newBounds)
                                node.labels.forEach { setLabelLayoutParameter(it, createLabelParamater(node)) }
                            }
                        }
                        override fun cancelDrag(p0: IInputModeContext?, p1: PointD?) {}
                        override fun getType() = HandleTypes.MOVE!!
                        override fun getCursor() = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)!!
                    }
                    listOf(tipHandle)
                }
            }
            else -> super.lookup(node, type)
        }
    }
}
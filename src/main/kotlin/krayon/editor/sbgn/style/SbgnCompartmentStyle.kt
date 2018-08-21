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
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.Pen
import com.yworks.yfiles.view.input.*
import java.awt.Color


class SbgnCompartmentStyle : SbgnShapeNodeStyle() {

    @Suppress("MemberVisibilityCanBePrivate")
    var cornerX:Double = 60.0
    @Suppress("MemberVisibilityCanBePrivate")
    var cornerY:Double = 15.0

    init {
        pen = Pen(Color.BLACK, 4.0)
    }

    override fun isHit(context: IInputModeContext, p: PointD, node: INode): Boolean {
        val superHit = super.isHit(context, p, node)
        val isInsideOnlyMode =
                context.parentInputMode is CreateEdgeInputMode ||
                context.parentInputMode is MarqueeSelectionInputMode ||
                context.parentInputMode is ClickInputMode ||
                context.parentInputMode is PopupMenuInputMode ||
                context.parentInputMode is ItemHoverInputMode
        return if(superHit && isInsideOnlyMode) {
            val insideBox = node.layout.toRectD().getEnlarged(InsetsD.fromLTRB(-10.0,-cornerY,-10.0,-cornerY))
            !insideBox.contains(p)
        }
        else superHit
    }

    override fun isInBox(context: IInputModeContext?, box: RectD, node: INode): Boolean {
        return box.contains(node.layout.topLeft) && box.contains(node.layout.bottomRight)
    }

    override fun createGeneralPath(node:INode, size: SizeD): GeneralPath {
        val cx = Math.min(cornerX, size.width * 0.3)
        val cy = Math.min(cornerY, size.height * 0.3)

        return GeneralPath(16).apply {
            moveTo(0.0,cy)
            cubicTo(0.0,0.0, cx,0.0, cx,0.0)
            lineTo(size.width-cx, 0.0)
            cubicTo(size.width-cx,0.0, size.width, 0.0, size.width, cy)
            lineTo(size.width, size.height-cy)
            cubicTo(size.width,size.height, size.width-cx, size.height, size.width-cx, size.height)
            lineTo(cx, size.height)
            cubicTo(cx, size.height, 0.0, size.height, 0.0, size.height-cy)
            close()
        }
    }
}

/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.GeneralPath
import com.yworks.yfiles.geometry.Matrix2D
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import krayon.editor.sbgn.model.SbgnPropertyKey
import krayon.editor.sbgn.model.getSbgnProperty
import kotlin.math.PI

class SbgnTagStyle : SbgnShapeNodeStyle() {

    private var prevOrientation:String = "none"

    override fun createGeneralPath(node: INode, size: SizeD): GeneralPath {
        val path = GeneralPath(10)

        val orientation = node.getSbgnProperty(SbgnPropertyKey.ORIENTATION) ?: "right"
        when(orientation) {
            "left", "right" -> {
                val tip = Math.min(size.height / Math.sqrt(2.0), size.width * 0.5)
                path.moveTo(0.0, 0.0)
                path.lineTo(size.width - tip, 0.0)
                path.lineTo(size.width, size.height * 0.5)
                path.lineTo(size.width - tip, size.height)
                path.lineTo(0.0, size.height)
                path.close()
                if (orientation == "left") {
                    path.transform(Matrix2D().apply { rotate(PI); translate(PointD(-size.width, -size.height)) })
                }
            }
            else -> { //create down
                val tip = Math.min(size.width / Math.sqrt(2.0), size.height * 0.5)
                path.moveTo(0.0, 0.0)
                path.lineTo(0.0, size.height - tip)
                path.lineTo(size.width * 0.5, size.height)
                path.lineTo(size.width, size.height - tip)
                path.lineTo(size.width, 0.0)
                path.close()
                if (orientation == "up") {
                    path.transform(Matrix2D().apply { rotate(PI); translate(PointD(-size.width, -size.height)) })
                }
            }
        }
        return path
    }

    override fun updateVisual(context: IRenderContext, visual: IVisual, node: INode): IVisual {
        val orientation =  node.getSbgnProperty(SbgnPropertyKey.ORIENTATION) as? String ?: "right"
        if(prevOrientation != orientation) requiresVisualUpdate = true
        prevOrientation = orientation
        return super.updateVisual(context, visual, node)
    }

}

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
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.INode
import krayon.editor.base.util.arcTo

open class SbgnSimpleChemicalStyle : SbgnMultimerStyle() {

    override fun createShape(node: INode, size: SizeD): GeneralPath {
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

    override fun getOutline(node: INode): GeneralPath {
        val x = node.layout.x
        val y = node.layout.y
        val width = node.layout.width
        val height = node.layout.height
        val radius = height / 2
        return GeneralPath().apply {
            moveTo(x+radius,y)
            arcTo(radius, x+radius, y+radius, 1.5*Math.PI, 0.5*Math.PI)
            lineTo(x+width-radius,y+height)
            arcTo(radius, x+width-radius, y+radius, 2.5*Math.PI, 1.5*Math.PI)
            close()
        }
    }
}

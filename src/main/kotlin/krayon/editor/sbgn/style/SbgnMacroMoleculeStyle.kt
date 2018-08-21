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

open class SbgnMacroMoleculeStyle : SbgnMultimerStyle() {

    override fun createShape(node:INode, size: SizeD):GeneralPath {
        val shape = GeneralPath(12)
        val w = size.width
        val h = size.height
        val arcX = Math.min(w * 0.5, shapeParameter)
        val arcY = Math.min(h * 0.5, shapeParameter)
        shape.moveTo(0.0, arcY)
        shape.quadTo(0.0, 0.0, arcX, 0.0)
        shape.lineTo(w - arcX, 0.0)
        shape.quadTo( w, 0.0, w, arcY)
        shape.lineTo(w, h - arcY)
        shape.quadTo(w, h, w - arcX, h)
        shape.lineTo(arcX, h)
        shape.quadTo(0.0, h, 0.0, h - arcY)
        shape.close()
        return shape
    }
}

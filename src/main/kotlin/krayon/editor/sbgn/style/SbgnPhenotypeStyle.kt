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

class SbgnPhenotypeStyle : SbgnMultimerStyle() {

    init {
        shapeParameter = 15.0
    }

    override fun createShape(node: INode, size: SizeD): GeneralPath {
        val w = size.width
        val h = size.height
        return GeneralPath(12).apply {
            moveTo(shapeParameter, 0.0)
            lineTo(w-shapeParameter, 0.0)
            lineTo(w, h*0.5)
            lineTo(w-shapeParameter, h)
            lineTo(shapeParameter, h)
            lineTo(0.0, h*0.5)
            close()
        }
    }
}
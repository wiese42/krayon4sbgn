/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.MutableRectangle
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.view.VisualGroup
import krayon.editor.base.util.toRectD
import krayon.editor.base.util.toRectangle2D

class VisualGroupWithBounds : VisualGroup(), IVisualWithBounds {
    override val bounds: RectD
        get() {
            val box = MutableRectangle()
            for (child in children) {
                (child as? IVisualWithBounds)?.let {
                    if(transform == null)
                        box.add(it.bounds)
                    else
                        box.add(transform.createTransformedShape(it.bounds.toRectangle2D()).bounds2D.toRectD())
                }
            }
            return box.toRectD()
        }
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.Pen
import krayon.editor.base.ui.create
import java.awt.*

class ShapeVisualWithBounds(var shape:Shape, var pen: Pen?= Pen.getBlack(), var paint: Paint? = null): IVisualWithBounds {

    override val bounds get() = with(shape.bounds2D) { RectD(x,y,width,height) }

    fun update(shape:Shape, pen:Pen?, paint:Paint? = null) {
        this.shape = shape
        this.pen = pen
        this.paint = paint
    }

    override fun paint(context: IRenderContext, g: Graphics2D) {
        g.create { gfx ->
            paint?.let {
                gfx.paint = it
                gfx.fill(shape)
            }
            pen?.let {
                it.adopt(gfx)
                gfx.draw(shape)
            }
        }
    }
}
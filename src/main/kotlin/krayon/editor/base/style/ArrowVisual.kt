/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.ISize
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import java.awt.Graphics2D
import java.awt.geom.AffineTransform

abstract class ArrowVisual : IVisual {
    // moves and rotates the graphics context to the current location and orientation of its edge's endpoint
    private val transform: AffineTransform = AffineTransform()

    /**
     * Updates the location and orientation of the edge's endpoint.
     */
    fun update(anchor: PointD, direction: PointD) {
        transform.setTransform(direction.getX(), direction.getY(), -direction.getY(), direction.getX(), anchor.getX(),
                anchor.getY())
    }

    abstract fun paintArrow(context: IRenderContext, gfx: Graphics2D)
    abstract fun getSize():ISize

    override fun paint(context: IRenderContext, gfx: Graphics2D) {
        val oldTransform = gfx.transform
        gfx.transform(transform)
        paintArrow(context, gfx)
        gfx.transform = oldTransform
    }
}


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
import com.yworks.yfiles.view.IVisual
import com.yworks.yfiles.view.IVisualTemplate
import com.yworks.yfiles.view.Pen
import krayon.editor.base.ui.create
import java.awt.geom.GeneralPath

open class HighlightNodeVisualTemplate(private val pen:Pen, var tagVisual:IVisualWithBounds? = null): IVisualTemplate {

    override fun createVisual(context: IRenderContext, bounds: RectD, obj: Any?): IVisual {
        return IVisual { _, g -> g.create { gfx ->
            val gap = 5.0
            val ratio = 0.2
            with(bounds) {
                pen.adopt(gfx)
                //top left
                gfx.draw(GeneralPath().apply {
                    moveTo(x+ratio*width, y-gap)
                    lineTo(x-gap, y-gap)
                    lineTo(x-gap, y+ratio*height)
                })
                //top right
                gfx.draw(GeneralPath().apply {
                    moveTo(maxX-ratio*width, y-gap)
                    lineTo(maxX+gap, y-gap)
                    lineTo(maxX+gap, y+ratio*height)
                })
                //bottom left
                gfx.draw(GeneralPath().apply {
                    moveTo(x+ratio*width, maxY+gap)
                    lineTo(x-gap, maxY+gap)
                    lineTo(x-gap, maxY-ratio*height)
                })
                //bottom right
                gfx.draw(GeneralPath().apply {
                    moveTo(maxX-ratio*width, maxY+gap)
                    lineTo(maxX+gap, maxY+gap)
                    lineTo(maxX+gap, maxY-ratio*height)
                })

                if(tagVisual != null) {
                    val tagBounds = tagVisual!!.bounds
                    gfx.translate(x+(width-tagBounds.width)*0.5,y-pen.thickness*0.5-2*gap-tagBounds.maxY)
                    tagVisual!!.paint(context, gfx)
                }
            }
        } }
    }

    override fun updateVisual(context: IRenderContext, oldVisual: IVisual, bounds: RectD, obj: Any?): IVisual {
        return createVisual(context, bounds, obj)
    }
}
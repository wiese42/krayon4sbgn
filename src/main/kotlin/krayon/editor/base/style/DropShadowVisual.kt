/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.jhlabs.image.GaussianFilter
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.utils.ImageSupport
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisual
import krayon.editor.base.ui.create
import krayon.editor.base.util.Cache
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.image.BufferedImage
import kotlin.math.max

class DropShadowVisual : IVisual {

    class DropShadowParam(val hOffset:Int, val vOffset:Int, val blurRadius: Int, val color:Color)

    private val params:MutableList<DropShadowParam> = mutableListOf()
    private var shadowImage:BufferedImage? = null
    private var prevSize:SizeD? = null

    private var imageOffsetX:Int = 0
    private var imageOffsetY:Int = 0

    @Suppress("unused")
    constructor(hOffset: Int, vOffset: Int, blurRadius: Int, color: Color) {
        params.add(DropShadowParam(hOffset, vOffset, blurRadius, color))
    }

    constructor(vararg param:DropShadowParam) {
        params.addAll(param)
    }

    //shape needs to be (0,0) positioned
    fun update(size: SizeD, shape: Shape, cacheHint:Any? = null) {
        if(cacheHint != null) {
            val cachedImage = imageCache[Pair(size, cacheHint)]
            if(cachedImage != null) {
                shadowImage = cachedImage
                prevSize = size
                imageOffsetX = params.map { max(0,it.blurRadius-it.hOffset) }.max()!!
                imageOffsetY = params.map { max(0,it.blurRadius-it.vOffset) }.max()!!
                return
            }
        }


        if(shadowImage == null || prevSize != size) {
            //println("updating...size=$size cacheHint=$cacheHint shape=${shape.bounds}")
            shadowImage = null
            val width = size.width.toInt()
            val height = size.height.toInt()
            imageOffsetX = params.map { max(0,it.blurRadius-it.hOffset) }.max()!!
            imageOffsetY = params.map { max(0,it.blurRadius-it.vOffset) }.max()!!
            val imageWidth = width + imageOffsetX + params.map { max(0,it.blurRadius+it.hOffset) }.max()!!
            val imageHeight = height + imageOffsetY + params.map { max(0,it.blurRadius+it.vOffset) }.max()!!

            params.forEach { with(it) {
                val srcImage = ImageSupport.createBufferedImage(width+2*blurRadius, height+2*blurRadius, true)
                val gfx = srcImage.graphics as Graphics2D
                gfx.color = color
                gfx.translate(blurRadius, blurRadius)
                gfx.fill(shape)
                gfx.translate(-blurRadius, -blurRadius)
                val filter = GaussianFilter()
                filter.useAlpha = true
                filter.radius = blurRadius.toFloat()
                val blurredImage = filter.createCompatibleDestImage(srcImage, srcImage.colorModel)
                filter.filter(srcImage, blurredImage)

                if(params.size > 1) {  //merge into one image
                    shadowImage = shadowImage ?: ImageSupport.createBufferedImage(imageWidth, imageHeight, true)
                    val shadowGfx = shadowImage!!.graphics as Graphics2D
                    shadowGfx.drawImage(blurredImage, imageOffsetX + hOffset - blurRadius, imageOffsetY + vOffset - blurRadius, null)
                }
                else {
                    shadowImage = blurredImage
                    imageOffsetX = blurRadius - hOffset
                    imageOffsetY = blurRadius - vOffset
                }
            }}
            cacheHint?.let { imageCache[Pair(size,it)] = shadowImage!! }
        }
        prevSize = size
    }

    override fun paint(context: IRenderContext, g: Graphics2D) {
        g.create { gfx ->
            gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            gfx.drawImage(shadowImage, -imageOffsetX, -imageOffsetY, null)
        }
    }

    companion object {
        val imageCache = Cache.createWeakCache<Pair<SizeD,Any>, BufferedImage>()
        //val imageCache = Cache.createSoftCache<Pair<SizeD,Any>, BufferedImage>(5)
        //val imageCache = Cache.createNoCache<Pair<SizeD,Any>, BufferedImage>()

        @Suppress("unused")
        fun createTestDropShadow():DropShadowVisual {
            return DropShadowVisual(
                DropShadowVisual.DropShadowParam(-20, 0, 10, Color(1f, 0f, 0f, 0.2f)), //red-left
                DropShadowVisual.DropShadowParam(0, -20, 10, Color(0f, 1f, 0f, 0.2f)), //green-top
                DropShadowVisual.DropShadowParam(20, 0, 10, Color(0f, 0f, 1f, 0.2f)), //blue-right
                DropShadowVisual.DropShadowParam(0, 20, 10, Color(0f, 0f, 0f, 0.2f))) //black-bottom
        }
    }
}
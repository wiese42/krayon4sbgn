/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.ISize
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.styles.Arrow
import com.yworks.yfiles.graph.styles.IArrow
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.Pen
import krayon.editor.base.style.ArrowVisual
import krayon.editor.base.style.ArrowVisualArrow
import krayon.editor.sbgn.model.SbgnType
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.GeneralPath
import java.awt.geom.Line2D

object SbgnArrows {

    fun create(type: SbgnType, pen: Pen? = null, scaleX:Double = 1.0, scaleY:Double = 1.0, gap:Double = 3.0): IArrow {
        return when(type) {
            SbgnType.PRODUCTION -> createProduction(pen, pen?.paint, scaleX, scaleY, gap)
            SbgnType.MODULATION -> createModulation(pen, null, scaleX, scaleY, gap)
            SbgnType.STIMULATION -> createStimulation(pen, null, scaleX, scaleY, gap)
            SbgnType.CATALYSIS -> createCatalysis(pen, null, scaleX, scaleY, gap)
            SbgnType.INHIBITION -> createInhibition(pen, scaleY, gap)
            SbgnType.NECESSARY_STIMULATION -> createNecessaryStimulation(pen, null, scaleX, scaleY, gap)
            else -> Arrow.NONE
        }
    }

    fun createProduction(pen: Pen? = Pen.getBlack(), paint: Paint? = Color.BLACK, scaleX:Double = 1.0, scaleY:Double = 1.0, gap:Double = 3.0): IArrow {
        return ArrowVisualArrow({
            val shape = GeneralPath().apply {
                moveTo(-10.0, -5.0)
                lineTo(-10.0, 5.0)
                lineTo(0.0, 0.0)
                closePath()
                transform(AffineTransform.getScaleInstance(scaleX, scaleY))
            }
            val size = SizeD(10.0 * scaleX, 10.0 * scaleY)

            object : ArrowVisual() {
                override fun paintArrow(context: IRenderContext, gfx: Graphics2D) {
                    if (paint != null) {
                        gfx.paint = paint
                        gfx.fill(shape)
                    }
                    if (pen != null) {
                        pen.adopt(gfx)
                        gfx.draw(shape)
                    }
                }

                override fun getSize(): ISize {
                    return size
                }
            }
        }, gap, 10.0 * scaleY)
    }

    fun createNecessaryStimulation(pen: Pen? = Pen.getBlack(), paint: Paint? = null, scaleX:Double = 1.0, scaleY:Double = 1.0, gap:Double = 3.0): IArrow {
        return ArrowVisualArrow({
            val shape = GeneralPath().apply {
                moveTo(-10.0, -5.0)
                lineTo(-10.0, 5.0)
                lineTo(0.0, 0.0)
                closePath()
                transform(AffineTransform.getScaleInstance(0.7 * scaleX, 0.7 * scaleY))
            }
            val bar = Line2D.Double(-10.0 * scaleX, -5.0 * scaleY, -10.0 * scaleX, 5.0 * scaleY)
            val size = SizeD(10.0 * scaleX, 10.0 * scaleY)

            object : ArrowVisual() {
                override fun paintArrow(context: IRenderContext, gfx: Graphics2D) {
                    if (paint != null) {
                        gfx.paint = paint
                        gfx.fill(shape)
                    }
                    if (pen != null) {
                        pen.adopt(gfx)
                        gfx.draw(shape)
                        gfx.draw(bar)
                    }
                }

                override fun getSize(): ISize {
                    return size
                }
            }
        }, gap, 7.0 * scaleY)
    }

    fun createInhibition(pen: Pen? = Pen.getBlack(), scaleY:Double = 1.0, gap:Double = 3.0): IArrow {
        return ArrowVisualArrow({
            val bar = Line2D.Double(0.0, -5.0 * scaleY, 0.0, 5.0 * scaleY)
            val size = SizeD(5.0, 10.0 * scaleY)
            object : ArrowVisual() {
                override fun paintArrow(context: IRenderContext, gfx: Graphics2D) {
                    if (pen != null) {
                        pen.adopt(gfx)
                        gfx.draw(bar)
                    }
                }

                override fun getSize(): ISize {
                    return size
                }
            }
        }, gap)
    }

    fun createStimulation(pen: Pen? = Pen.getBlack(), paint: Paint? = null, scaleX:Double = 1.0, scaleY:Double = 1.0, gap:Double = 3.0): IArrow {
        return createProduction(pen, paint, scaleX * 0.9, scaleY * 0.9, gap)
    }

    fun createModulation(pen: Pen? = Pen.getBlack(), paint: Paint? = null, scaleX:Double = 1.0, scaleY:Double = 1.0, gap:Double = 3.0): IArrow {
        return ArrowVisualArrow({
            val shape = GeneralPath().apply {
                moveTo(-10.0, 0.0)
                lineTo(-5.0, 5.0)
                lineTo(0.0, 0.0)
                lineTo(-5.0, -5.0)
                closePath()
                transform(AffineTransform.getScaleInstance(scaleX * 0.9, scaleY * 0.9))
            }
            val size = SizeD(10.0 * scaleX, 10.0 * scaleY)

            object : ArrowVisual() {
                override fun paintArrow(context: IRenderContext, gfx: Graphics2D) {
                    if (paint != null) {
                        gfx.paint = paint
                        gfx.fill(shape)
                    }
                    if (pen != null) {
                        pen.adopt(gfx)
                        gfx.draw(shape)
                    }
                }

                override fun getSize(): ISize {
                    return size
                }
            }
        }, gap, 9.0 * scaleY)
    }

    fun createCatalysis(pen: Pen? = Pen.getBlack(), paint: Paint? = null, scaleX:Double = 1.0, scaleY:Double = 1.0, gap:Double = 3.0): IArrow {
        return ArrowVisualArrow({
            val shape = GeneralPath().apply {
                append(Ellipse2D.Double(-9.0, -4.5, 9.0, 9.0), true)
                transform(AffineTransform.getScaleInstance(scaleX, scaleY))
            }
            val size = SizeD(9.0 * scaleX, 9.0 * scaleY)

            object : ArrowVisual() {
                override fun paintArrow(context: IRenderContext, gfx: Graphics2D) {
                    if (paint != null) {
                        gfx.paint = paint
                        gfx.fill(shape)
                    }
                    if (pen != null) {
                        pen.adopt(gfx)
                        gfx.draw(shape)
                    }
                }

                override fun getSize(): ISize {
                    return size
                }
            }
        }, gap, 9.0 * scaleY)
    }
}
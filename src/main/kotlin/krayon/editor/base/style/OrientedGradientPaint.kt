/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import krayon.editor.base.util.minus
import krayon.editor.base.util.plus
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.ColorModel
import java.util.*

class OrientedGradientPaint(val degAngle:Double, val fractions:FloatArray, val colors:Array<Color>) : Paint {

    private val _transparency:Int
    private val startPoint:Point2D

    init {
        var opaque = true
        for (i in colors.indices) {
            opaque = opaque && colors[i].alpha == 0xff
        }
        _transparency = if (opaque) Transparency.OPAQUE else Transparency.TRANSLUCENT

        startPoint = Point2D.Double(-1.0, 0.0)
        AffineTransform.getRotateInstance(Math.toRadians(degAngle)).transform(startPoint, startPoint)
    }

    override fun createContext(cm: ColorModel, deviceBounds: Rectangle, userBounds: Rectangle2D, xform: AffineTransform, hints: RenderingHints): PaintContext {
        //println("createContext")
        return transformToRect(RectD(userBounds.x, userBounds.y, userBounds.width, userBounds.height)).createContext(cm, deviceBounds, userBounds, xform, hints)
    }

    override fun getTransparency(): Int {
        //println("getTransparency")
        return _transparency
    }

    private fun transformToRect(rect: RectD): LinearGradientPaint {
        val scale = rect.width*rect.height
        val rotatedStart = PointD(startPoint.x*scale,  startPoint.y*scale)
        val boundaryStart = rect.findLineIntersection(rect.center, rotatedStart+ PointD(rect.width*0.5, rect.height*0.5)) ?: rect.topLeft
        val delta = rect.center - boundaryStart
        val boundaryEnd = rect.center + delta
        val start = Point2D.Float(boundaryStart.x.toFloat(), boundaryStart.y.toFloat())
        val end = Point2D.Float(boundaryEnd.x.toFloat(), boundaryEnd.y.toFloat())
        return LinearGradientPaint(start,end, fractions,colors)
    }

    override fun equals(other: Any?): Boolean {
        if(other !is OrientedGradientPaint) return false
        if(other.degAngle != degAngle || other.colors.size != colors.size) return false
        for(i in 0..colors.lastIndex) {
            if(other.colors[i] != other.colors[i] || other.fractions[i] != fractions[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var result = degAngle.hashCode()
        result = 31 * result + Arrays.hashCode(fractions)
        result = 31 * result + Arrays.hashCode(colors)
        return result
    }
}
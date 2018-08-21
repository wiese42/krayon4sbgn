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
import com.yworks.yfiles.view.ICanvasContext
import com.yworks.yfiles.view.IVisual
import com.yworks.yfiles.graph.styles.IArrow
import com.yworks.yfiles.view.IBoundsProvider
import com.yworks.yfiles.view.IRenderContext
import com.yworks.yfiles.view.IVisualCreator
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.IEdge

open class ArrowVisualArrow(private val visualFactory: () -> ArrowVisual, private val gap: Double = 1.0, private val length: Double = 0.0) : IArrow, IVisualCreator, IBoundsProvider {

    // these variables hold the state for the transformation and differ from ArrowVisual to ArrowVisual; they are
    // populated in getVisualCreator and getBoundsProvider and assigned to the ArrowVisual in createVisual() and
    // updateVisual()
    // the location of the arrow
    private var anchor: PointD? = null
    // the orientation of the arrow
    private var direction: PointD? = null
    private var size: ISize = visualFactory().getSize()

    /**
     * Returns the length of the arrow, i.e. the distance from the arrow's tip to the position where the visual
     * representation of the edge's path should begin. Always returns 0.
     */
    override fun getLength(): Double {
        return length
    }

    /**
     * Gets the cropping length associated with this instance. Always returns 1. This value is used by [ ]s to let the edge appear to end shortly before its actual target.
     */
    override fun getCropLength(): Double {
        return gap
    }

    /**
     * Gets an [com.yworks.yfiles.view.IVisualCreator] implementation that will create the visual
     * for this arrow at the given location using the given orientation for the given edge.
     * @param edge      the edge this arrow belongs to
     * @param atSource  whether this will be the source arrow
     * @param anchor    the anchor point for the tip of the arrow
     * @param direction the orientation the arrow is pointing in
     * @return itself as a flyweight
     */
    override fun getVisualCreator(edge: IEdge, atSource: Boolean, anchor: PointD, direction: PointD): IVisualCreator {
        this.anchor = anchor
        this.direction = direction
        return this
    }

    /**
     * Gets an [com.yworks.yfiles.view.IBoundsProvider] implementation that can yield this arrow's bounds if
     * painted at the given location using the given orientation for the given edge.
     * @param edge      the edge this arrow belongs to
     * @param atSource  whether this will be the source arrow
     * @param anchor    the anchor point for the tip of the arrow
     * @param direction the orientation the arrow is pointing in
     * @return an implementation of the [com.yworks.yfiles.view.IBoundsProvider] interface that can subsequently
     * be used to query the bounds. Clients will always call this method before using the implementation and may not cache
     * the instance returned. This allows for applying the flyweight design pattern to implementations.
     */
    override fun getBoundsProvider(edge: IEdge, atSource: Boolean, anchor: PointD, direction: PointD): IBoundsProvider {
        this.anchor = anchor
        this.direction = direction
        return this
    }


    /**
     * This method is called by the framework to create a [IVisual] that will
     * be included into the [com.yworks.yfiles.view.IRenderContext].
     * @param context the context that describes where the visual will be used
     * @return the arrow visual to include in the canvas object visual tree
     */
    override fun createVisual(context: IRenderContext): IVisual {
        val visual = visualFactory()
        visual.update(anchor!!, direction!!)
        size = visual.getSize()
        return visual
    }

    /**
     * The [com.yworks.yfiles.view.CanvasComponent] uses this method to give implementations a chance to update an
     * existing visual that has previously been created by the same instance during a call to [ ][.createVisual].
     */
    override fun updateVisual(context: IRenderContext, group: IVisual): IVisual {
        val visual = group as ArrowVisual
        visual.update(anchor!!, direction!!)
        size = visual.getSize()
        return visual
    }

    /**
     * Returns the bounds of the arrow for the current flyweight configuration.
     */
    override fun getBounds(context: ICanvasContext): RectD {
        return RectD(anchor!!.getX() - size.width-gap, anchor!!.getY() - size.height * 0.5, size.width, size.height)
    }

}


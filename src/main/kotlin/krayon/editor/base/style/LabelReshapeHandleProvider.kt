/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.IMutableRectangle
import com.yworks.yfiles.geometry.OrientedRectangle
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.FreeNodeLabelModel
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterFinder
import com.yworks.yfiles.view.input.*

class LabelReshapeHandleProvider(val label:ILabel, positions: HandlePositions):IReshapeHandleProvider {

    private val labelMutableRect = LabelMutableRectangle(label)
    private val delegateProvider = RectangleReshapeHandleProvider(labelMutableRect, positions)

    override fun getAvailableHandles(context: IInputModeContext): HandlePositions {
        return  delegateProvider.getAvailableHandles(context)
    }

    override fun getHandle(context: IInputModeContext, positon: HandlePositions): IHandle {
        labelMutableRect.graph = context.graph
        return delegateProvider.getHandle(context, positon)
    }

    class LabelMutableRectangle(val label: ILabel) : IMutableRectangle {

        var graph:IGraph? = null
        private fun getLabelBounds() = label.layoutParameter.model.getGeometry(label, label.layoutParameter).bounds!!

        override fun getY(): Double {
            return getLabelBounds().y
        }

        override fun setX(x: Double) {
            val oldBounds = getLabelBounds()
            updateLabelBounds(x, oldBounds.y, oldBounds.width, oldBounds.height)
        }

        override fun getHeight(): Double {
            return getLabelBounds().height
        }

        override fun getX(): Double {
            return getLabelBounds().x
        }

        override fun setY(y: Double) {
            val oldBounds = getLabelBounds()
            updateLabelBounds(oldBounds.x, y, oldBounds.width, oldBounds.height)
        }

        override fun getWidth(): Double {
            return getLabelBounds().width
        }

        override fun setWidth(width: Double) {
            val oldBounds = getLabelBounds()
            updateLabelBounds(oldBounds.x, oldBounds.y, width, oldBounds.height)
            graph!!.setLabelPreferredSize(label, SizeD(width,oldBounds.height))
        }

        override fun setHeight(height: Double) {
            val oldBounds = getLabelBounds()
            updateLabelBounds(oldBounds.x, oldBounds.y, oldBounds.width, height)
            graph!!.setLabelPreferredSize(label, SizeD(oldBounds.width,height))
        }

        private fun updateLabelBounds(x:Double, y:Double, width:Double, height:Double) {
            val paramFinder = label.layoutParameter.model as? ILabelModelParameterFinder
            val newParam = if(paramFinder != null) {
                paramFinder.findBestParameter(label, label.layoutParameter.model, OrientedRectangle(RectD(x,y,width,height)))
            }
            else {
                FreeNodeLabelModel.INSTANCE.createCanonicalParameter((label.owner as INode).layout.toRectD(), OrientedRectangle(RectD(x,y,width, height)))
            }
            graph!!.setLabelLayoutParameter(label, newParam)
        }

    }
}
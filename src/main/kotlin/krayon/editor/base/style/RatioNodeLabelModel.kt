/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.*
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.ILookup
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.*
import krayon.editor.base.util.clamp
import krayon.editor.base.util.convertToRatioPoint

class RatioNodeLabelModel(private val constraint:ILabelModelParameterFinderConstraint? = null) : ILabelModelParameterFinder, ILabelModel, ILookup {

    private val freeModel = FreeNodeLabelModel.INSTANCE!!

    override fun findBestParameter(label: ILabel, model: ILabelModel, box: IOrientedRectangle): ILabelModelParameter {
        return createParameter((label.owner as INode).layout.toRectD(), constraint?.constrainLayout(label, box) ?: box)
    }

    fun createParameter(nodeLayout: IRectangle, layout: IOrientedRectangle):ILabelModelParameter {
        var refPoint = nodeLayout.convertToRatioPoint(layout.bounds.center)
        refPoint = PointD(refPoint.clamp(PointD.ORIGIN, PointD(1.0, 1.0)))
        return freeModel.createParameter(refPoint, PointD.ORIGIN, PointD(0.5, 0.5), PointD.ORIGIN, 0.0).wrapped()
    }

    fun createParameter(nodeLayoutRatio: PointD):ILabelModelParameter {
        return freeModel.createParameter(nodeLayoutRatio, PointD.ORIGIN, PointD(0.5, 0.5), PointD.ORIGIN, 0.0).wrapped()
    }

    override fun createDefaultParameter(): ILabelModelParameter {
        return freeModel.createDefaultParameter().wrapped()
    }

    override fun getGeometry(label: ILabel, param: ILabelModelParameter): IOrientedRectangle {
        return freeModel.getGeometry(label, param.unwrapped())
    }

    override fun getContext(p0: ILabel?, p1: ILabelModelParameter?): ILookup {
        return ILookup.EMPTY
    }

    override fun <T : Any> lookup(type: Class<T>): T? {
        if(type == ILabelModelParameterFinder::class.java) {
            @Suppress("UNCHECKED_CAST")
            return this as T
        }
        return freeModel.lookup(type)
    }

    private fun ILabelModelParameter.wrapped():ILabelModelParameter {
        return LabelModelParameterWrapper(this)
    }

    private fun ILabelModelParameter.unwrapped():ILabelModelParameter {
        return (this as LabelModelParameterWrapper).paramDelegate
    }

    inner class LabelModelParameterWrapper(var paramDelegate:ILabelModelParameter) : ILabelModelParameter, Cloneable {
        override fun clone(): Any {
            val wrapper = super.clone() as LabelModelParameterWrapper
            wrapper.paramDelegate = paramDelegate.clone() as ILabelModelParameter
            return wrapper
        }

        override fun supports(label: ILabel): Boolean {
            return paramDelegate.supports(label)
        }

        override fun getModel(): ILabelModel {
            return this@RatioNodeLabelModel
        }
    }

    companion object {
        val INSTANCE = RatioNodeLabelModel()
    }
}
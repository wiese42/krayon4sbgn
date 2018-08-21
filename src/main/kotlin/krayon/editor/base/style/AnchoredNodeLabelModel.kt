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
import krayon.editor.base.util.convertToRatioPoint
import java.lang.Math.abs

class AnchoredNodeLabelModel(private val constraint: ILabelModelParameterFinderConstraint? = null) : ILabelModelParameterFinder, ILabelModel, ILookup {

    private val freeModel = FreeNodeLabelModel.INSTANCE!!

    override fun findBestParameter(label: ILabel, model: ILabelModel, box: IOrientedRectangle): ILabelModelParameter {
        return createParameter((label.owner as INode).layout.toRectD(), constraint?.constrainLayout(label, box) ?: box)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createParameter(nodeLayout: IRectangle, layout: IOrientedRectangle):ILabelModelParameter {
        val refPoint = nodeLayout.convertToRatioPoint(layout.bounds.center)
        val anchor = MutablePoint()
        val labelOffset = MutablePoint()
        //println("refPoint=$refPoint")
        when {
            refPoint.x < 0.33 -> {
                anchor.x = 0.0
                labelOffset.x = -refPoint.x * nodeLayout.width + layout.width * 0.5
            }
            refPoint.x > 0.66 -> {
                anchor.x = 1.0
                labelOffset.x = (1.0-refPoint.x)*nodeLayout.width - layout.width * 0.5
            }
            else -> {
                anchor.x = 0.5
                labelOffset.x = nodeLayout.center.x - layout.bounds.centerX
            }
        }
        when {
            refPoint.y < 0.33 -> {
                anchor.y = 0.0
                labelOffset.y = -refPoint.y * nodeLayout.height + layout.height * 0.5
            }
            refPoint.y > 0.66 -> {
                anchor.y = 1.0
                labelOffset.y =  (1.0-refPoint.y)*nodeLayout.height - layout.height * 0.5
            }
            else -> {
                anchor.y = 0.5
                labelOffset.y = nodeLayout.center.y - layout.bounds.centerY
            }
        }

        return freeModel.createParameter(anchor.toPointD(), PointD.ORIGIN, anchor.toPointD(), labelOffset.toPointD(), 0.0).wrapped()
    }

    fun createParameter(position:InteriorLabelModel.Position, insetsD: InsetsD = InsetsD.EMPTY):ILabelModelParameter {
        val (anchor, labelOffset) = when(position) {
            InteriorLabelModel.Position.NORTH_EAST -> {
                Pair(PointD(1.0, 0.0), PointD(insetsD.right, -insetsD.top))
            }
            InteriorLabelModel.Position.SOUTH -> {
                Pair(PointD(0.5, 1.0), PointD(0.0, insetsD.bottom))
            }
            InteriorLabelModel.Position.NORTH -> {
                Pair(PointD(0.5, 0.0), PointD(0.0, -insetsD.top))
            }
            InteriorLabelModel.Position.CENTER -> {
                Pair(PointD(0.5, 0.5), PointD((insetsD.right-insetsD.left)*0.5,(insetsD.bottom-insetsD.top)*0.5))
            }
            InteriorLabelModel.Position.EAST -> {
                Pair(PointD(1.0, 0.5), PointD(-insetsD.left, 0.0))
            }
            InteriorLabelModel.Position.NORTH_WEST -> {
                Pair(PointD(0.0, 0.0), PointD(-insetsD.left, -insetsD.top))
            }
            InteriorLabelModel.Position.SOUTH_EAST -> {
                Pair(PointD(1.0, 1.0), PointD(-insetsD.left, insetsD.bottom))
            }
            InteriorLabelModel.Position.SOUTH_WEST -> {
                Pair(PointD(0.0, 1.0), PointD(insetsD.right, insetsD.bottom))
            }
            InteriorLabelModel.Position.WEST -> {
                Pair(PointD(1.0, 0.0), PointD(insetsD.right, 0.0))
            }
        }
        return freeModel.createParameter(anchor, PointD.ORIGIN, anchor, labelOffset, 0.0).wrapped()
    }

    fun createParameter(position:ExteriorLabelModel.Position, insetsD: InsetsD = InsetsD.EMPTY):ILabelModelParameter {
        val (anchor, labelOffset) = when(position) {
            ExteriorLabelModel.Position.NORTH_EAST -> {
                Pair(PointD(1.0, 0.0), PointD(-insetsD.left, insetsD.bottom))
            }
            ExteriorLabelModel.Position.SOUTH -> {
                Pair(PointD(0.5, 1.0), PointD(0.0, -insetsD.top))
            }
            ExteriorLabelModel.Position.NORTH -> {
                Pair(PointD(0.5, 0.0), PointD(0.0, insetsD.bottom))
            }
            ExteriorLabelModel.Position.EAST -> {
                Pair(PointD(1.0, 0.5), PointD(insetsD.right, 0.0))
            }
            ExteriorLabelModel.Position.NORTH_WEST -> {
                Pair(PointD(0.0, 0.0), PointD(insetsD.right, insetsD.bottom))
            }
            ExteriorLabelModel.Position.SOUTH_EAST -> {
                Pair(PointD(1.0, 1.0), PointD(-insetsD.left, -insetsD.top))
            }
            ExteriorLabelModel.Position.SOUTH_WEST -> {
                Pair(PointD(0.0, 1.0), PointD(insetsD.right, -insetsD.top))
            }
            ExteriorLabelModel.Position.WEST -> {
                Pair(PointD(1.0, 0.0), PointD(-insetsD.left, 0.0))
            }
        }
        val labelAnchor = PointD(abs(anchor.x-1.0), abs(anchor.y-1.0))
        return freeModel.createParameter(anchor, PointD.ORIGIN, labelAnchor, labelOffset, 0.0).wrapped()
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
            return this@AnchoredNodeLabelModel
        }

        override fun toString(): String {
            return "AnchoredNodeLabelModel.Param: ${unwrapped()}"
        }
    }

    companion object {
        val INSTANCE = AnchoredNodeLabelModel()
    }
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.IOrientedRectangle
import com.yworks.yfiles.geometry.OrientedRectangle
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.INode
import krayon.editor.base.util.minus
import krayon.editor.base.util.plus
import krayon.editor.base.util.times

interface ILabelModelParameterFinderConstraint {
    fun constrainLayout(label: ILabel, layout: IOrientedRectangle): IOrientedRectangle
}

class OutlineConstraint : ILabelModelParameterFinderConstraint {
    override fun constrainLayout(label: ILabel, layout: IOrientedRectangle): IOrientedRectangle {
        val node = label.owner as INode
        val shapeGeom = node.style.renderer.getShapeGeometry(node, node.style)
        val labelCenter = layout.center
        if(labelCenter != node.layout.center) {
            val box = shapeGeom.outline.bounds
            val length = PointD(0.5*box.width, 0.5*box.height).vectorLength
            val outsideP = node.layout.center + (labelCenter - node.layout.center).normalized * (length+1)
            val borderPoint = shapeGeom.getIntersection(node.layout.center, outsideP)
            if(borderPoint != null) {
                return OrientedRectangle(RectD.fromCenter(borderPoint, layout.toSizeD()))
            }
        }
        return layout
    }
}

class InteriorConstraint : ILabelModelParameterFinderConstraint {
    override fun constrainLayout(label: ILabel, layout: IOrientedRectangle): IOrientedRectangle {
        val node = label.owner as INode
        val shapeGeom = node.style.renderer.getShapeGeometry(node, node.style)
        val labelCenter = layout.center
        if(shapeGeom.isInside(labelCenter)) return layout
        val borderPoint = shapeGeom.getIntersection(node.layout.center, labelCenter)
        if(borderPoint != null) {
            return OrientedRectangle(RectD.fromCenter(borderPoint, layout.toSizeD()))
        }
        return layout
    }
}

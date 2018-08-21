/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.IEdge
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.IPort
import com.yworks.yfiles.view.input.INodeHitTester
import krayon.editor.base.model.IEdgeCreationHint
import krayon.editor.base.ui.ConstrainedCreateEdgeInputMode
import krayon.editor.base.util.*
import krayon.editor.sbgn.layout.RotateTransformation
import krayon.editor.sbgn.model.*
import krayon.editor.sbgn.style.SbgnBuilder

class SbgnCreateEdgeInputMode : ConstrainedCreateEdgeInputMode<SbgnType>() {

    override fun configureTargetNode(target: INode, targetType: SbgnType, location:PointD, edgeHint: IEdgeCreationHint<SbgnType>) {
        super.configureTargetNode(target, targetType, location, edgeHint)

        getCompartmentAtLocation(location)?.let {
            graph.setParent(target, it)
        }

        if(constraintManager.isNodeAcceptingLabel(target, SbgnType.NAME_LABEL)) {
            SbgnBuilder.addNameLabel(graph, target)
        }
        SbgnBuilder.configure(graph, target)
        graph.setNodeCenter(target, location)

        val refPoint = dummyEdge.bends.lastOrNull()?.location?.toPointD() ?: startPoint
        val rc = inputModeContext.graphComponent.createRenderContext()

        //val targetType = typeModel.getType(target)
        when {
            targetType.isPN() -> when {
                edgeHint.type.isRegulation() -> {
                    target.setSbgnProperty(SbgnPropertyKey.ORIENTATION, "horizontal")
                    SbgnBuilder.addPorts(graph, target)
                    if(refPoint.isInLeftOuterSector(target.layout) || refPoint.isInRightOuterSector(target.layout)) {
                        RotateTransformation.rotateNode(graph, target, rc)
                    }
                }
                else -> {
                    target.setSbgnProperty(SbgnPropertyKey.ORIENTATION, "horizontal")
                    SbgnBuilder.addPorts(graph, target)
                    if(refPoint.isInBottomOuterSector(target.layout) || refPoint.isInTopOuterSector(target.layout)) {
                        RotateTransformation.rotateNode(graph, target, rc)
                    }
                }
            }
            targetType.isLogic() -> {
                target.orientation = "vertical"
                SbgnBuilder.addPorts(graph, target)
                if(refPoint.isInLeftOuterSector(target.layout) || refPoint.isInRightOuterSector(target.layout)) {
                    RotateTransformation.rotateNode(graph, target, rc)
                }
            }
            targetType == SbgnType.TAG -> {
                target.orientation = "right"
                if(!refPoint.isInRightOuterSector(target.layout)) {
                    RotateTransformation.rotateNode(graph, target, rc)
                    if(!refPoint.isInBottomOuterSector(target.layout)) {
                        RotateTransformation.rotateNode(graph, target, rc)
                        if(!refPoint.isInLeftOuterSector(target.layout)) {
                            RotateTransformation.rotateNode(graph, target, rc)
                        }
                    }
                }
            }
        }

        val prevalentSize = SbgnBuilder.getPrevalentSize(graph, targetType, target.orientation, target)
        if(prevalentSize != null) {
            graph.setNodeLayout(target, RectD.fromCenter(target.center, prevalentSize))
        }
    }

    private fun getCompartmentAtLocation(location: PointD): INode? {
        return inputModeContext.lookup(INodeHitTester::class.java).enumerateHits(inputModeContext, location).firstOrNull {
            it.type == SbgnType.COMPARTMENT
        }
    }

    override fun getPortForTargetNode(target: INode, edgeHint: IEdgeCreationHint<SbgnType>): IPort {
        val targetType = typeModel.getType(target)
        val refPoint = dummyEdge.bends.lastOrNull()?.location?.toPointD() ?: startPoint
        val targetPort = when {
            targetType.isPN() -> when {
                edgeHint.type.isRegulation() -> {
                    target.ports.filter { it.type != SbgnType.INPUT_AND_OUTPUT }.minBy { it.location.distanceTo(refPoint)}
                }
                else -> {
                    target.ports.filter { it.type == SbgnType.INPUT_AND_OUTPUT }.minBy { it.location.distanceTo(refPoint)}
                }
            }
            targetType.isLogic() -> {
                target.ports.minBy { it.location.distanceTo(refPoint)}
            }
            else -> graph.addPort(target)
        }
        return targetPort ?: super.getPortForTargetNode(target, edgeHint)
    }

    override fun configureTypeLabelForDummyEdge(dummyEdge: IEdge) {
        when(typeModel.getType(dummyEdge)) {
            SbgnType.LOGIC_ARC -> setTypeLabelToDummyEdge(dummyEdge, "Logic")
            SbgnType.EQUIVALENCE_ARC -> setTypeLabelToDummyEdge(dummyEdge, "Equiv")
            else -> removeTypeLabelFromDummyEdge(dummyEdge)
        }
    }
}
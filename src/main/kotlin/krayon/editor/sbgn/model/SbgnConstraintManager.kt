/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.model

import com.yworks.yfiles.graph.IEdge
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.IPort
import com.yworks.yfiles.view.input.IInputModeContext
import krayon.editor.base.model.IEdgeCreationHint
import krayon.editor.base.model.IModelConstraintManager
import krayon.editor.base.util.isAncestor
import krayon.editor.sbgn.ui.SbgnGraphComponent

data class SbgnEdgeCreationHint(override val type: SbgnType, override val reversed: Boolean = false) : IEdgeCreationHint<SbgnType> {
    override fun toString(): String = "hint: type=$type  reversed=$reversed"
}

val IInputModeContext.sbgnConstraintManager get() = (canvasComponent as SbgnGraphComponent).constraintManager

object SbgnConstraintManager : IModelConstraintManager<SbgnType> {

    enum class ConstraintLevel {
        STRICT,
        NONE
    }

    var constraintLevel = ConstraintLevel.STRICT

    override fun getPreferredTargetType(graph:IGraph, source:INode, sourcePort:IPort?, arcType: SbgnType): SbgnType {
        val sourceType = source.type
        //println("getPTT sourceType=$sourceType  arcType=$arcType")
        return when {
            arcType.isRegulation() -> SbgnType.PROCESS
            arcType == SbgnType.LOGIC_ARC -> SbgnType.AND
            arcType == SbgnType.EQUIVALENCE_ARC -> {
                when (sourceType) {
                    SbgnType.TAG -> SbgnType.SIMPLE_CHEMICAL
                    SbgnType.SUBMAP -> SbgnType.SIMPLE_CHEMICAL
                    else -> SbgnType.TAG
                }
            }
            arcType == SbgnType.CONSUMPTION -> SbgnType.PROCESS
            arcType == SbgnType.PRODUCTION -> getPreferredProductionType(graph, source)
            else -> SbgnType.SIMPLE_CHEMICAL
        }
    }

    private fun getPreferredProductionType(graph:IGraph, source:INode):SbgnType {
        if(source.type == SbgnType.ASSOCIATION) return SbgnType.COMPLEX
        val inTypes = graph.inEdgesAt(source).filter { it.type == SbgnType.CONSUMPTION }.map { it.sourceNode.type }
        val outTypes = graph.outEdgesAt(source).filter { it.type == SbgnType.PRODUCTION }.map { it.targetNode.type }
        return if(inTypes.any { it == SbgnType.SIMPLE_CHEMICAL }) SbgnType.SIMPLE_CHEMICAL
        else if(inTypes.any { it == SbgnType.MACROMOLECULE } && outTypes.none { it == SbgnType.MACROMOLECULE }) SbgnType.MACROMOLECULE
        else if(inTypes.any { it == SbgnType.NUCLEIC_ACID_FEATURE } && outTypes.none { it == SbgnType.NUCLEIC_ACID_FEATURE }) SbgnType.NUCLEIC_ACID_FEATURE
        else SbgnType.SIMPLE_CHEMICAL
    }

    override fun getPreferredSourceType(graph:IGraph, target:INode, targetPort:IPort?, arcType: SbgnType): SbgnType {
        val targetType = target.type
        //println("getPST sourceType=$targetType  arcType=$arcType")
        return when {
            arcType.isRegulation() -> SbgnType.MACROMOLECULE
            arcType == SbgnType.LOGIC_ARC -> SbgnType.MACROMOLECULE
            arcType == SbgnType.EQUIVALENCE_ARC -> {
                when (targetType) {
                    SbgnType.TAG -> SbgnType.SIMPLE_CHEMICAL
                    SbgnType.SUBMAP -> SbgnType.SIMPLE_CHEMICAL
                    else -> SbgnType.TAG
                }
            }
            arcType == SbgnType.CONSUMPTION -> getPreferredConsumptionType(graph, target)
            else -> SbgnType.PROCESS
        }
    }

    private fun getPreferredConsumptionType(graph:IGraph, target:INode):SbgnType {
        if(target.type == SbgnType.DISSOCIATION) return SbgnType.COMPLEX
        val outTypes = graph.outEdgesAt(target).filter { it.type == SbgnType.PRODUCTION }.map { it.targetNode.type }
        val inTypes = graph.inEdgesAt(target).filter { it.type == SbgnType.CONSUMPTION }.map { it.sourceNode.type }
        return if(outTypes.any { it == SbgnType.SIMPLE_CHEMICAL }) SbgnType.SIMPLE_CHEMICAL
        else if(outTypes.any { it == SbgnType.MACROMOLECULE } && inTypes.none { it == SbgnType.MACROMOLECULE }) SbgnType.MACROMOLECULE
        else if(outTypes.any { it == SbgnType.NUCLEIC_ACID_FEATURE } && inTypes.none { it == SbgnType.NUCLEIC_ACID_FEATURE }) SbgnType.NUCLEIC_ACID_FEATURE
        else SbgnType.SIMPLE_CHEMICAL
    }

    override fun getEdgeCreationHints(graph:IGraph, source:INode, sourcePort: IPort?):List<IEdgeCreationHint<SbgnType>> {
        return getEdgeConversionHints(graph, source, sourcePort, null)
    }

    override fun getEdgeConversionHints(graph:IGraph, edge:IEdge):List<IEdgeCreationHint<SbgnType>> {
        val sourceHints = getEdgeConversionHints(graph, edge.sourceNode, edge.sourcePort, edge)
        if(constraintLevel == ConstraintLevel.NONE) return sourceHints

        val targetHints = getEdgeConversionHints(graph, edge.targetNode, edge.targetPort, edge)
        return sourceHints.intersect(targetHints.map { SbgnEdgeCreationHint(it.type, !it.reversed) }).toList()
    }

    private fun getEdgeConversionHints(graph:IGraph, source:INode, sourcePort: IPort?, edge:IEdge?):List<IEdgeCreationHint<SbgnType>> {
        if(constraintLevel == ConstraintLevel.NONE) {
            return SbgnType.values().filter { it.isArc() }.map { SbgnEdgeCreationHint(it, false) } // + SbgnType.values().filter { it.isArc() }.map { SbgnEdgeCreationHint(it, true) }
        }

        val sourceType = source.type
        val sourcePortType = sourcePort?.type
        val regulation = listOf(SbgnType.CATALYSIS, SbgnType.STIMULATION, SbgnType.INHIBITION, SbgnType.NECESSARY_STIMULATION, SbgnType.MODULATION)
        val reversed = true
        val hints = mutableListOf<SbgnEdgeCreationHint>()
        when {
            graph.getParent(source)?.type?.isComplex() == true -> {
                hints += SbgnEdgeCreationHint(SbgnType.MODULATION)
            }
            sourceType.isEPN() -> {
                hints += SbgnEdgeCreationHint(SbgnType.CONSUMPTION)
                hints += SbgnEdgeCreationHint(SbgnType.PRODUCTION, reversed)
                hints += (listOf(SbgnType.LOGIC_ARC, SbgnType.EQUIVALENCE_ARC) + regulation).map { SbgnEdgeCreationHint(it) }
            }
            sourceType.isPN() && sourcePortType == SbgnType.INPUT_AND_OUTPUT -> {
                val outEdgesAtPort = graph.outEdgesAt(sourcePort).filter { it != edge }.count()
                val inEdgesAtPort = graph.inEdgesAt(sourcePort).filter { it != edge }.count()
                val productionAtOtherPort = graph.outEdgesAt(source).any { it != edge && it.type == SbgnType.PRODUCTION && it.sourcePort !== sourcePort }
                val consumptionAtOtherPort = graph.inEdgesAt(source).any { it != edge && it.type == SbgnType.CONSUMPTION && it.targetPort !== sourcePort }
                //println("outEdgesAtPort=$outEdgesAtPort  production@other=$productionAtOtherPort   consumption@other=$consumptionAtOtherPort")
                when {
                    inEdgesAtPort > 0 -> hints += SbgnEdgeCreationHint(SbgnType.CONSUMPTION, reversed)
                    outEdgesAtPort > 0 -> hints += SbgnEdgeCreationHint(SbgnType.PRODUCTION)
                    consumptionAtOtherPort -> hints += SbgnEdgeCreationHint(SbgnType.PRODUCTION)
                    productionAtOtherPort -> {  //prefer consumption when other os production
                        hints += SbgnEdgeCreationHint(SbgnType.CONSUMPTION, reversed)
                        hints += SbgnEdgeCreationHint(SbgnType.PRODUCTION)
                    }
                    else -> { //untouched
                        hints += SbgnEdgeCreationHint(SbgnType.PRODUCTION)
                        hints += SbgnEdgeCreationHint(SbgnType.CONSUMPTION, reversed)
                    }
                }
            }
            sourceType.isPN() && sourcePortType != SbgnType.INPUT_AND_OUTPUT -> {
                hints += regulation.map { SbgnEdgeCreationHint(it, reversed) }
                //hints += SbgnEdgeCreationHint(SbgnType.LOGIC_ARC, reversed)
            }
            sourceType == SbgnType.COMPARTMENT || sourceType == SbgnType.SUBMAP || sourceType == SbgnType.TAG ->
                hints += SbgnEdgeCreationHint(SbgnType.EQUIVALENCE_ARC)
            sourceType.isLogic() -> {
                when {
                    graph.outEdgesAt(sourcePort).any { edge != it && it.type.isRegulation() } -> {
                        //no hints
                    }
                    graph.outEdgesAt(sourcePort).any { edge != it && it.type == SbgnType.LOGIC_ARC } -> hints += SbgnEdgeCreationHint(SbgnType.LOGIC_ARC)
                    graph.inEdgesAt(sourcePort).any { edge != it && it.type == SbgnType.LOGIC_ARC } -> hints += SbgnEdgeCreationHint(SbgnType.LOGIC_ARC, reversed)
                    graph.outEdgesAt(source).any { edge != it && it.sourcePort !== sourcePort } -> hints += SbgnEdgeCreationHint(SbgnType.LOGIC_ARC, reversed)
                    else -> {
                        hints += regulation.map { SbgnEdgeCreationHint(it) }
                        hints += SbgnEdgeCreationHint(SbgnType.LOGIC_ARC)
                    }
                }
            }
            sourceType == SbgnType.PHENOTYPE -> {
                hints += regulation.map { SbgnEdgeCreationHint(it, reversed) }
            }
        }
        return hints
    }

    override fun isNodeAcceptingPort(nodeType: SbgnType, portType: SbgnType): Boolean {
        return portType == SbgnType.TERMINAL && nodeType == SbgnType.SUBMAP
    }

    override fun isEdgeAcceptingLabel(edge:IEdge, labelType: SbgnType): Boolean {
        return when {
            constraintLevel == ConstraintLevel.NONE -> true
            edge.labels.any { it.type == SbgnType.CARDINALITY } -> false
            labelType == SbgnType.CARDINALITY-> edge.type == SbgnType.CONSUMPTION || edge.type == SbgnType.PRODUCTION
            else -> false
        }
    }

    override fun isNodeAcceptingLabel(node:INode, labelType: SbgnType): Boolean {
        if(constraintLevel == ConstraintLevel.NONE && labelType != SbgnType.NAME_LABEL) return true

        val nodeType = node.type
        return when(labelType) {
            SbgnType.UNIT_OF_INFORMATION -> (nodeType.isEPN() && nodeType != SbgnType.SOURCE_AND_SINK) || nodeType == SbgnType.COMPARTMENT || nodeType == SbgnType.PHENOTYPE
            SbgnType.STATE_VARIABLE -> (nodeType.isEPN() && nodeType != SbgnType.SOURCE_AND_SINK) || nodeType == SbgnType.PHENOTYPE
            SbgnType.NAME_LABEL -> (nodeType.isEPN() && nodeType != SbgnType.SOURCE_AND_SINK) || nodeType == SbgnType.COMPARTMENT || nodeType == SbgnType.PHENOTYPE || nodeType.isReference()
            SbgnType.CALLOUT_LABEL -> true
            else -> false
        }
    }

    override fun getNodeConversionTypes(graph:IGraph, node:INode):List<SbgnType> {
        val allowedTypes = SbgnType.values().filter { nodeType -> nodeType.isNode() &&
                graph.outEdgesAt(node).all { edge -> isValidSource(graph, edge.targetNode, edge.type, node, nodeType, edge.sourcePort) } &&
                graph.inEdgesAt(node).all { edge -> isValidTarget(graph, edge.sourceNode, edge.type, node, nodeType, edge.targetPort) }
        }
        val weightMap = mapOf(
                SbgnType.SIMPLE_CHEMICAL to 1,
                SbgnType.MACROMOLECULE to 2,
                SbgnType.NUCLEIC_ACID_FEATURE to 3,
                SbgnType.PROCESS to 4,
                SbgnType.TAG to 5,
                SbgnType.AND to 6
        )
        return allowedTypes.sortedBy { weightMap[it] ?: 1000 }
    }

    override fun isValidTarget(graph:IGraph, source: INode, edgeType: SbgnType, target: INode, targetType: SbgnType, targetPort: IPort?): Boolean {
        if(constraintLevel == ConstraintLevel.NONE) return !graph.isAncestor(source,target) && !graph.isAncestor(target,source)
        val edgeCheck = when {
            graph.getParent(target)?.type?.isComplex() == true -> false  //complex member cannot be targets
            edgeType == SbgnType.CONSUMPTION -> targetPort?.type == SbgnType.INPUT_AND_OUTPUT && targetType.isPN() && graph.outDegree(targetPort) == 0
            edgeType == SbgnType.PRODUCTION  -> targetType.isEPN()
            edgeType.isRegulation() -> targetType != SbgnType.DISSOCIATION && targetType != SbgnType.ASSOCIATION && targetType.isPN() && targetPort?.type != SbgnType.INPUT_AND_OUTPUT || targetType == SbgnType.PHENOTYPE
            edgeType == SbgnType.LOGIC_ARC -> targetType.isLogic()
            edgeType == SbgnType.EQUIVALENCE_ARC -> when {
                source.type.isReference() -> targetType.isEPN() || targetType == SbgnType.COMPARTMENT
                source.type.isEPN() || source.type == SbgnType.COMPARTMENT -> targetType.isReference()
                else -> false
            }
            else -> false
        }
        return if(!edgeCheck) false
        else when {
            //make sure PN has no inEdges at opposite INPUT_AND_OUTPUT port.
            targetType.isPN() -> targetPort?.type != SbgnType.INPUT_AND_OUTPUT || graph.inEdgesAt(getOppositeInOutPort(target, targetPort)).count() == 0
            targetType == SbgnType.SOURCE_AND_SINK -> graph.degree(target) <= 1
            targetType == SbgnType.NOT -> targetPort != null && graph.inDegree(targetPort) <= 1
            targetType.isEPN() -> !graph.groupingSupport.getPathToRoot(target).any{ it != target && it.type.isComplex() }
            else -> true
        }
    }

    private fun getOppositeInOutPort(node:INode, port:IPort):IPort {
        return node.ports.find { it.type == SbgnType.INPUT_AND_OUTPUT && it != port }!!
    }

    override fun isValidSource(graph:IGraph, target: INode, edgeType: SbgnType, source:INode, sourceType:SbgnType, sourcePort: IPort?): Boolean {
        if(constraintLevel == ConstraintLevel.NONE) return true
        val edgeCheck = when {
            edgeType == SbgnType.CONSUMPTION -> sourceType.isEPN()
            edgeType == SbgnType.PRODUCTION -> sourcePort?.type == SbgnType.INPUT_AND_OUTPUT
            edgeType.isRegulation() -> (sourceType.isEPN() && sourceType != SbgnType.SOURCE_AND_SINK) || sourceType.isLogic()
            edgeType == SbgnType.LOGIC_ARC -> sourceType.isEPN() || sourceType.isLogic()
            edgeType == SbgnType.EQUIVALENCE_ARC -> when {
                sourceType.isReference() -> target.type.isEPN() || target.type == SbgnType.COMPARTMENT
                sourceType.isEPN() || source.type == SbgnType.COMPARTMENT -> target.type.isReference()
                else -> false
            }
            else -> false
        }

        return if(!edgeCheck) false
        else when {
            sourceType == SbgnType.ASSOCIATION -> graph.outEdgesAt(sourcePort).count { it.sourcePort.type == SbgnType.INPUT_AND_OUTPUT } <= 1
            sourceType == SbgnType.DISSOCIATION -> graph.inEdgesAt(sourcePort).count { it.sourcePort.type == SbgnType.INPUT_AND_OUTPUT } <= 1
            sourceType == SbgnType.SOURCE_AND_SINK -> graph.degree(source) <= 1
            sourceType.isLogic() -> graph.outDegree(sourcePort) <= 1
            graph.getParent(source)?.type?.isComplex() == true -> edgeType == SbgnType.MODULATION  //only modulation allowed
            else -> true
        }

    }

    override fun isValidEdgeConversion(graph:IGraph, edge:IEdge, type: SbgnType):Boolean {
        if(constraintLevel == ConstraintLevel.NONE) return true
        return getEdgeConversionHints(graph, edge).any { it.type == type }
    }

    override fun isValidChild(graph: IGraph, groupNodeType:SbgnType, node: INode): Boolean {
        val x =  when {
            //even ContraintLEvel.NONE shouldn't make compartment nesting possible
            //since this will potentially confuse SBGN readers.
            groupNodeType == SbgnType.COMPARTMENT -> node.type != SbgnType.COMPARTMENT
            constraintLevel == ConstraintLevel.NONE -> true
            groupNodeType.isComplex() -> node.type.canBeContainedInComplex() && graph.inDegree(node) == 0 && graph.outEdgesAt(node).all { it.type == SbgnType.MODULATION }
            else -> true
        }
        if(!x) {
            //println("validChild=false  constraintlevel=$constraintLevel")
        }
        return x
    }

    override fun isMergeable(aGraph:IGraph, aNode:INode, targetGraph:IGraph, targetNode:INode):Boolean {
        return when {
            aNode.type == SbgnType.COMPARTMENT || targetNode.type == SbgnType.COMPARTMENT -> false
        //aGraph.getParent(aNode)?.type?.isComplex() == true -> false
            aGraph.degree(aNode) == 0 -> getNodeConversionTypes(aGraph, aNode).any { it == targetNode.type }
            else -> when {  //edge transfer.
                targetGraph.getParent(targetNode)?.type?.isComplex() == true && ((aGraph.inDegree(aNode) > 0 || aGraph.outEdgesAt(aNode).any { it.type !=  SbgnType.MODULATION})) -> false
                aNode.type.isEPN() -> targetNode.type.isEPN()
                aNode.type.isPN() -> targetNode.type.isPN() && {
                    val aOutCount = aNode.ports.count { it.type == SbgnType.INPUT_AND_OUTPUT && aGraph.outDegree(it) > 0 }
                    val aInCount = aNode.ports.count { it.type == SbgnType.INPUT_AND_OUTPUT && aGraph.inDegree(it) > 0 }
                    val targetOutCount = targetNode.ports.count { it.type == SbgnType.INPUT_AND_OUTPUT && targetGraph.outDegree(it) > 0 }
                    val targetInCount = targetNode.ports.count { it.type == SbgnType.INPUT_AND_OUTPUT && targetGraph.inDegree(it) > 0 }
                    aOutCount < 2 && targetOutCount < 2 || aOutCount == 2 && targetInCount == 0 || targetOutCount == 2 && aInCount == 0
                }.invoke()
                else -> false
            }
        }
    }

    override fun isMergeablePort(aGraph:IGraph, aPort:IPort, targetGraph: IGraph, targetPort: IPort):Boolean {
        return when {
            aPort.type == SbgnType.INPUT_AND_OUTPUT && aGraph.outDegree(aPort) > 0 -> {
                targetPort.type == SbgnType.INPUT_AND_OUTPUT && targetGraph.inDegree(targetPort) == 0
            }
            aPort.type == SbgnType.INPUT_AND_OUTPUT && aGraph.inDegree(aPort) > 0 -> {
                targetPort.type == SbgnType.INPUT_AND_OUTPUT && targetGraph.outDegree(targetPort) == 0
            }
            else -> aPort.type == targetPort.type
        }
    }
}

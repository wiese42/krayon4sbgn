/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.model

import com.yworks.yfiles.geometry.IPoint
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.IEdge
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.IPort
import com.yworks.yfiles.graph.styles.ILabelStyle
import com.yworks.yfiles.utils.ICloneable
import com.yworks.yfiles.view.input.IInputModeContext
import krayon.editor.base.util.*
import krayon.editor.sbgn.style.SbgnBuilder

object SbgnMergeManager {

    /**
     * transfers edges from one node to another.
     * precondition: nodes are mergeable
     */
    fun transferEdgesToNode(context:IInputModeContext, graph: IGraph, transferSource: INode, transferTarget: INode) {
        graph.beginEdit().use { _ ->
            val transferPorts = transferTarget.ports.toList()
            transferSource.ports.toList().forEach { aPort ->
                if(graph.degree(aPort) > 0) {
                    val hitPort = transferPorts.filter { targetPort ->
                        context.sbgnConstraintManager.isMergeablePort(graph, aPort, graph, targetPort)
                    }.let {
                        if(aPort.type != SbgnType.NO_TYPE) getClosestPort(it, graph, aPort)
                        else {
                            if(isCenterPort(aPort)) {
                                getOrCreateCenterPort(graph, transferTarget)
                            }
                            else {
                                val rp = transferSource.layout.convertToRatioPoint(aPort.location)
                                graph.addPort(transferTarget, transferTarget.layout.convertFromRatioPoint(rp))
                            }
                        }
                    } ?: graph.addPort(transferTarget)

                    graph.edgesAt(aPort).toList().forEach { edge ->
                        if (edge.sourceNode == transferSource) {
                            graph.setEdgePorts(edge, hitPort, edge.targetPort)
                        } else {
                            graph.setEdgePorts(edge, edge.sourcePort, hitPort)
                        }
                    }
                }
            }
        }
    }

    private fun isCenterPort(port: IPort): Boolean {
        return port.location.distanceTo((port.owner as INode).center) < 1.0
    }

    private fun getOrCreateCenterPort(graph: IGraph, node: INode): IPort {
        return node.ports.find { it.location.distanceTo(node.layout.center) < 1.0 } ?: graph.addPort(node)
    }

    fun mergeNodeFeatures(context:IInputModeContext, graph: IGraph, node: INode, templateNode: INode, newSize: SizeD, newLabelText:String?) {

        //if(node.type == templateNode.type) return

        graph.beginEdit().use { _ ->
            val prevType = node.type
            node.tag = (templateNode.tag as ICloneable).clone()

            val nameLabel = node.getNameLabel()
            if(newLabelText != null && nameLabel != null && nameLabel.text != newLabelText) {
                graph.setLabelText(nameLabel, newLabelText)
            }

            val box = when(getUniqueEdgeSide(graph, node)) {
                "left" -> RectD.fromCenter(PointD(node.layout.x + newSize.width*0.5, node.center.y), newSize)
                "right" -> RectD.fromCenter(PointD(node.layout.maxX - newSize.width*0.5, node.center.y), newSize)
                "top" -> RectD.fromCenter(PointD(node.center.x, node.layout.y + newSize.height*0.5), newSize)
                "bottom" -> RectD.fromCenter(PointD(node.center.x, node.layout.maxY - newSize.height*0.5), newSize)
                else -> RectD.fromCenter(node.layout.center, newSize)
            }

            graph.setNodeLayout(node, box)


            SbgnBuilder.configure(graph, node, node.layout)

            // tweak ports and determine preferred orientation
            // --Regulation--> PN | Phenotype
            // EPN | Logic --Regulation-->
            // EPN | Logic --Logic-->
            val regulationInput = graph.inEdgesAt(node).filter { it.type.isRegulation() }
            val regulationOutput = graph.outEdgesAt(node).filter { it.type.isRegulation() }
            val logicOutput = graph.outEdgesAt(node).filter { it.type == SbgnType.LOGIC_ARC }
            if(prevType.isPN() && node.type == SbgnType.PHENOTYPE && regulationInput.any()) {
                routeToCenter(graph, node, regulationInput)
            }
            else if(prevType.isLogic() && node.type.isEPN() && regulationOutput.any()) {
                routeToCenter(graph, node, regulationOutput)
            }
            else if(prevType.isLogic() && node.type.isEPN() && logicOutput.any()) {
                routeToCenter(graph, node, logicOutput)
            }
            else if(prevType == SbgnType.PHENOTYPE && node.type.isPN() && regulationInput.any()) {
                adjustSizeAndOrientation(graph, node, regulationInput, atSource = false, portsOnMainAxis = false)
                routeToPorts(graph, node, regulationInput, atSource = false)
            }
            else if(prevType.isEPN() && node.type.isLogic() && regulationOutput.any()) {
                adjustSizeAndOrientation(graph, node, regulationOutput, atSource = true, portsOnMainAxis = true)
                routeToPorts(graph, node, regulationOutput, atSource = true)
            }
            else if(prevType.isEPN() && node.type.isLogic() && logicOutput.any()) {
                adjustSizeAndOrientation(graph, node, logicOutput, atSource = true, portsOnMainAxis = true)
                routeToPorts(graph, node, logicOutput, atSource = true)
            }
            else if(graph.degree(node) == 0){
                node.ports.toList().forEach(graph::remove)
                SbgnBuilder.addPorts(graph, node)
            }

            //remove incompatible labels
            node.labels.filter { !context.sbgnConstraintManager.isNodeAcceptingLabel(node, it.type) }.forEach(graph::remove)
            //add labels from palette node
            templateNode.labels.forEach { pLabel ->
                if(pLabel.type != SbgnType.NAME_LABEL || node.labels.none{ it.type == SbgnType.NAME_LABEL }) {
                    graph.addLabel(node, pLabel.text, pLabel.layoutParameter, pLabel.style.clone() as ILabelStyle).apply {
                        tag = pLabel.tag
                    }
                }
            }

            if(prevType.isComplex()) { //remove contents of complex target
                graph.groupingSupport.getDescendantsBottomUp(node).forEach(graph::remove)
            }

            //reparent contents of complex node
            if(templateNode.type.isComplex()) {
                val delta = node.layout.topLeft - templateNode.layout.topLeft
                graph.setIsGroupNode(node, true)
                graph.getChildren(templateNode).toList().forEach {
                    graph.setParent(it, node)
                    graph.setNodeLayout(it, it.layout.translate(delta))
                }
            }

        }
    }

    fun findMergePair(context: IInputModeContext, affectedNodesGraph:IGraph, affectedNodeSet:Set<INode>, focusPoint:PointD, affectedNodesOffset: IPoint = PointD.ORIGIN):Pair<INode, INode>? {

        var bestANode: INode? = null
        var bestHitNode: INode? = null
        var minDistSqr = Double.MAX_VALUE

        val affectedBounds = affectedNodesGraph.getBounds({ node -> affectedNodeSet.contains(node)}, false, false).translate(affectedNodesOffset.toPointD())
        val unaffectedNodes = context.graph.nodes.filter { node ->
            !affectedNodeSet.contains(node) && affectedBounds.intersects(node.layout.toRectD())
        }

        if(unaffectedNodes.isEmpty()) return null

        for (aNode in affectedNodeSet) {
            val aPoint = aNode.center + affectedNodesOffset.toPointD()
            val hitNode = unaffectedNodes
                .filter {
                    it.layout.contains(aPoint) && !affectedNodesGraph.isAncestor(aNode,it) && !context.graph.isAncestor(it, aNode)
                }
                .minBy { (it as INode).layout.center.distanceToSqr(focusPoint) }

            if (hitNode != null && context.sbgnConstraintManager.isMergeable(affectedNodesGraph, aNode, context.graph, hitNode)) {
                val distSqr = hitNode.layout.center.distanceToSqr(focusPoint)
                if (distSqr < minDistSqr) {
                    bestANode = aNode
                    bestHitNode = hitNode
                    minDistSqr = distSqr
                }
            }
        }
        if(bestHitNode != null && bestANode != null) {
            return Pair(bestANode, bestHitNode)
        }
        return null
    }


    private fun adjustSizeAndOrientation(graph: IGraph, node: INode, edges:List<IEdge>, atSource: Boolean, portsOnMainAxis: Boolean) {
        val prevOrientation = node.orientation
        node.orientation = getEdgeInducedOrientation(node, edges, atSource = atSource, portsOnMainAxis = portsOnMainAxis)
        if(prevOrientation != node.orientation) graph.setNodeLayout(node, RectD.fromCenter(node.layout.center, node.layout.toSizeD().swap()))
    }

    private fun routeToCenter(graph: IGraph, node: INode, edges:List<IEdge>) {
        val prevPorts = node.ports.toList()
        val newPort = graph.addPort(node)
        edges.forEach { edge ->
            if(edge.targetNode == node)
                graph.setEdgePorts(edge, edge.sourcePort, newPort)
            else
                graph.setEdgePorts(edge, newPort, edge.targetPort)
        }
        prevPorts.forEach(graph::remove)
    }

    /**
     * @return one of "top", "bottom", "left", "right", or null
     */
    fun getUniqueEdgeSide(graph: IGraph, node:INode):String? {
        var side:String? = null
        for (edge in graph.edgesAt(node)) {
            val p = getEdgeSectorPoint(edge, edge.sourceNode == node)
            when {
                p.isInBottomOuterSector(node.layout) -> {
                    if(side != null && side != "bottom") return  null
                    side = "bottom"
                }
                p.isInTopOuterSector(node.layout) -> {
                    if(side != null && side != "top") return  null
                    side = "top"
                }
                p.isInLeftOuterSector(node.layout) -> {
                    if(side != null && side != "left") return  null
                    side = "left"
                }
                p.isInRightOuterSector(node.layout) -> {
                    if(side != null && side != "right") return  null
                    side = "right"
                }
            }
        }
        return side
    }

    fun getEdgeTransferDelta(graph: IGraph, aNode:INode, hitNode:INode):PointD {
        val side = getUniqueEdgeSide(graph, aNode)
        return when (side) {
            null -> hitNode.center - aNode.center
            "left" -> hitNode.layout.centerLeft - aNode.layout.centerLeft
            "right" -> hitNode.layout.centerRight - aNode.layout.centerRight
            "top" -> hitNode.layout.centerTop - aNode.layout.centerTop
            "bottom" -> hitNode.layout.centerBottom - aNode.layout.centerBottom
            else -> hitNode.center - aNode.center
        }
    }

    private fun getEdgeSectorPoint(edge: IEdge, atSource: Boolean): PointD {
        val box = if(atSource) edge.sourceNode.layout else edge.targetNode.layout
        return if(atSource)
            edge.bends.firstOrNull{ !box.contains(it.location) }?.location?.toPointD() ?: edge.targetPort.location
        else
            edge.bends.lastOrNull{ !box.contains(it.location) }?.location?.toPointD() ?: edge.sourcePort.location
    }

    private fun getEdgeInducedOrientation(node: INode, edges: List<IEdge>, atSource: Boolean, portsOnMainAxis: Boolean):String {
        val biasUnit = if(portsOnMainAxis) 1 else -1
        val box = node.layout
        var horizontalBias = 0

        edges.forEach { edge ->
            val p = getEdgeSectorPoint(edge, atSource)
            if(p.isInLeftOuterSector(box) || p.isInRightOuterSector(box)) horizontalBias+=biasUnit
            else if(p.isInBottomOuterSector(box) || p.isInTopOuterSector(box)) horizontalBias-=biasUnit
        }
        return if(horizontalBias >= 0) "horizontal" else "vertical"
    }

    private fun routeToPorts(graph: IGraph, node: INode, edges:List<IEdge>, atSource:Boolean) {
        val prevPorts = node.ports.toList()
        val box = node.layout
        if(node.orientation == "vertical") {
            SbgnBuilder.addPorts(graph, node)
            edges.forEach { edge ->
                val p = getEdgeSectorPoint(edge, atSource)
                val port = if(p.y < box.center.y) {
                    node.ports.filter{ !prevPorts.contains(it) }.minBy { it.location.y }!!
                } else {
                    node.ports.filter{ !prevPorts.contains(it) }.maxBy { it.location.y }!!
                }
                if(atSource) graph.setEdgePorts(edge, port, edge.targetPort)
                else graph.setEdgePorts(edge, edge.sourcePort, port)
            }
        }
        else {
            SbgnBuilder.addPorts(graph, node)
            edges.forEach { edge ->
                val p = getEdgeSectorPoint(edge, atSource)
                val port = if(p.x < box.center.x) {
                    node.ports.filter{ !prevPorts.contains(it) }.minBy { it.location.x }!!

                } else {
                    node.ports.filter{ !prevPorts.contains(it) }.maxBy { it.location.x }!!
                }
                if(atSource) graph.setEdgePorts(edge, port, edge.targetPort)
                else graph.setEdgePorts(edge, edge.sourcePort, port)
            }
        }
        prevPorts.forEach(graph::remove)
    }


    private fun getClosestPort(ports:List<IPort>, aGraph:IGraph, aPort: IPort): IPort? {
        return if(aGraph.degree(aPort) == 0) null
        else ports.minBy { port ->
            val aEdgeAverage = aGraph.edgesAt(aPort).sumByDouble { aEdge ->
                getEdgeSectorPoint(aEdge, atSource = aEdge.sourcePort != aPort).distanceTo(port.location)
            } / aGraph.degree(aPort)
            aEdgeAverage
        }
    }


    fun removeTransferSourceAfterEdgeTransfer(graph:IGraph, node:INode) {
        if(node.type.isComplex()) {
            graph.groupingSupport.getDescendants(node).toList().forEach {
                graph.remove(it)
            }
        }
        graph.remove(node)
    }



}



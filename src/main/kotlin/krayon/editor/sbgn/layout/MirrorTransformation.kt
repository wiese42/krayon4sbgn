/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.layout

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterFinder
import krayon.editor.base.util.*
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.getNameLabel
import krayon.editor.sbgn.model.orientation
import krayon.editor.sbgn.model.type
import krayon.util.predicate

object MirrorTransformation {

    enum class MirrorAxis {
        HORIZONTAL,
        VERTICAL
    }

    private fun calcMirrorAxesByMagic(graph:IGraph, isMirrorNode: NodePredicate): PointD {
        val connectingEdges = graph.edges.filter {
            (isMirrorNode(it.sourceNode) && !isMirrorNode(it.targetNode) || !isMirrorNode(it.sourceNode) && isMirrorNode(it.targetNode)) &&
                    graph.getParent(it.sourceNode) == graph.getParent(it.targetNode)
        }

        return if(connectingEdges.size == 1) { //exactly one connecting edge. honor it
            val edge = connectingEdges.first()
            if(isMirrorNode(edge.sourceNode)) edge.sourcePort.location
            else edge.targetPort.location
        }
        else {
            calcMirrorAxesFromBounds(graph, isMirrorNode)
        }
    }

    private fun calcMirrorAxesFromBounds(graph:IGraph, isMirrorNode: NodePredicate): PointD {
        val mirroredDecendents = mutableSetOf<INode>()
        graph.nodes.forEach { node ->
            if(isMirrorNode(node) && graph.isGroupNode(node)) {
                graph.groupingSupport.getDescendants(node).forEach{ mirroredDecendents.add(it) }
            }
        }
        val bounds = graph.getBounds( { isMirrorNode(it) || mirroredDecendents.contains(it) })
        return bounds.center
    }

    private fun mirror(graph: IGraph, root: INode?, axis:MirrorAxis, mirrorAxis:Double, isMirrorNode: NodePredicate, isFixedNode: NodePredicate) {
        graph.getChildren(root).forEach { node ->
            if(isMirrorNode(node)) {
                val delta = if(axis == MirrorAxis.HORIZONTAL) {
                    val newX = 2.0*mirrorAxis - node.center.x
                    PointD(newX - node.center.x, 0.0)
                }
                else {
                    val newY = 2.0*mirrorAxis - node.center.y
                    PointD(0.0, newY - node.center.y)
                }

                if(node.type == SbgnType.TAG) {
                    node.getNameLabel()?.let {
                        mirrorNodeLabel(graph,it,axis)
                    }
                }

                graph.setNodeLayout(node, node.layout.translate(delta))


                mirrorOrientation(node, axis)
                node.ports.forEach { port ->
                    val newPortLocation = if(axis == MirrorAxis.HORIZONTAL)
                        PointD(node.layout.maxX - (port.location.x - node.layout.x), port.location.y)
                    else
                        PointD(port.location.x, node.layout.maxY - (port.location.y - node.layout.y))
                    graph.setPortLocationParameter(port, port.locationParameter.model.createParameter(node, newPortLocation))
                }
                if(graph.isGroupNode(node)) {
                    if(isFixedNode(node)) {
                        graph.groupingSupport.getDescendants(node).forEach { fixedChild ->
                            graph.setNodeLayout(fixedChild, fixedChild.layout.translate(delta))
                        }
                    }
                    else { //not fixed but a mirror candidate. recurse
                         mirror(graph, node, axis, mirrorAxis, isMirrorNode, isFixedNode)
                    }
                }
                graph.outEdgesAt(node).forEach { edge ->
                    if(isMirrorNode(edge.targetNode) && edge.bends.any()) { //mirror bend points
                        graph.beginEdit(edge.bends).use {
                            edge.bends.toList().forEach { bend ->
                                val newBendLocation = if(axis == MirrorAxis.HORIZONTAL)
                                    PointD(2.0*mirrorAxis - bend.location.x, bend.location.y)
                                else
                                    PointD(bend.location.x, 2.0*mirrorAxis - bend.location.y)
                                //println("location=${bend.location}  newLocation=$newBendLocation")
                                graph.setBendLocation(bend, newBendLocation)
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("MoveLambdaOutsideParentheses")
    fun mirror(graph: IGraph, mirrorNodes: Iterable<INode>, axis:MirrorAxis) {
        val isMirrorNode = mirrorNodes.toHashSet().predicate()
        val mirrorPools = mutableMapOf<INode?, MutableSet<INode>>()
        graph.nodes.forEach { node ->
            if(isMirrorNode(node)) {
                var parent = graph.getParent(node)
                while(parent != null && isMirrorNode(parent)) {
                    parent = graph.getParent(parent)
                }
                val mirrorPoolNodes = (mirrorPools[parent] ?: hashSetOf()).apply { add(node) }
                //println("node=$node  mirrorNodes=${mirrorNodes.size}")
                mirrorPools[parent] = mirrorPoolNodes
            }
        }
        mirrorPools.forEach { group, mirrorPoolNodes ->
            //println("mirrorPool group=$group  #poolNodes=${poolNodes.size}")
            val isMirrorPoolNode = mirrorPoolNodes.predicate()
            val mirrorAxis = calcMirrorAxesByMagic(graph, isMirrorPoolNode).let { if(axis == MirrorAxis.HORIZONTAL) it.x else it.y  }
            mirror(graph, group, axis, mirrorAxis, isMirrorPoolNode, { it.type.isComplex() })
        }
    }

    private fun mirrorOrientation(node:INode, mirrorAxis: MirrorAxis) {
        node.orientation = when (mirrorAxis) {
            MirrorAxis.HORIZONTAL -> when (node.orientation) {
                "right" -> "left"
                "left" -> "right"
                else -> node.orientation
            }
            MirrorAxis.VERTICAL -> when (node.orientation) {
                "up" -> "down"
                "down" -> "up"
                else -> node.orientation
            }
        }
    }

    private fun mirrorNodeLabel(graph:IGraph, label: ILabel, mirrorAxis: MirrorAxis) {
        (label.layoutParameter.model as? ILabelModelParameterFinder)?.let { finder ->
            val nodeLayout = (label.owner as INode).layout
            val centerRatioPoint = nodeLayout.convertToRatioPoint(label.layout.center)
            val newRatioPoint = when(mirrorAxis) {
                MirrorAxis.HORIZONTAL -> centerRatioPoint.withX(1.0-centerRatioPoint.x)
                MirrorAxis.VERTICAL -> centerRatioPoint.withY(1.0-centerRatioPoint.y)
            }
            val newCenter = nodeLayout.convertFromRatioPoint(newRatioPoint)
            graph.setLabelLayoutParameter(label, finder.findBestParameter(label, RectD.fromCenter(newCenter, label.layout.toSizeD())))
        }
    }
}
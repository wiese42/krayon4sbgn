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
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterFinder
import com.yworks.yfiles.view.IRenderContext
import krayon.editor.base.util.*
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.orientation
import krayon.editor.sbgn.model.type
import krayon.util.predicate

object RotateTransformation {

    enum class RotationDirection {
        COUNTER_CLOCKWISE,
        CLOCKWISE
    }

    fun rotate(graph: IGraph, rotationNodes: Iterable<INode>, direction:RotationDirection, renderContext:IRenderContext) {
        val isRotationNode = rotationNodes.toHashSet().predicate()
        val rotationPool = mutableMapOf<INode?, MutableSet<INode>>()
        graph.nodes.forEach { node ->
            if(isRotationNode(node)) {
                var parent = graph.getParent(node)
                while(parent != null && isRotationNode(parent)) {
                    parent = graph.getParent(parent)
                }
                val rotationPoolNodes = (rotationPool[parent] ?: hashSetOf()).apply { add(node) }
                rotationPool[parent] = rotationPoolNodes
            }
        }
        rotationPool.forEach { group, rotationPoolNodes ->
            val isRotationPoolNode = rotationPoolNodes.predicate()
            val rotationPoint = calcRotationPoint(graph, isRotationNode)

            rotationPoolNodes.forEach { rotateOrientation(it, direction) }

            rotate(graph, group, rotationPoint, direction, isRotationPoolNode,
                    { it.orientation == null && it.type != SbgnType.SUBMAP && it.type != SbgnType.COMPARTMENT }, { it.type.isComplex() },
                    renderContext)
        }
    }

    private fun rotateOrientation(node:INode, direction: RotationDirection) {
        node.orientation = when (direction) {
            RotationDirection.CLOCKWISE -> when (node.orientation) {
                "right" -> "down"
                "down" -> "left"
                "left" -> "up"
                "up" -> "right"
                "horizontal" -> "vertical"
                "vertical" -> "horizontal"
                else -> null
            }
            RotationDirection.COUNTER_CLOCKWISE -> when (node.orientation) {
                "right" -> "up"
                "up" -> "left"
                "left" -> "down"
                "down" -> "right"
                "horizontal" -> "vertical"
                "vertical" -> "horizontal"
                else -> null
            }
        }
    }

    private fun calcRotationPoint(graph: IGraph, isRotationNode: NodePredicate): PointD {
        val connectionEdges = graph.edges.filter {
            isRotationNode(it.sourceNode) && !isRotationNode(it.targetNode) ||
                    !isRotationNode(it.sourceNode) && isRotationNode(it.targetNode) }
        val pivotNodes = connectionEdges.map {
            if(isRotationNode(it.sourceNode)) it.targetNode else it.sourceNode
        }.toSet()
        return if(pivotNodes.size == 1) pivotNodes.first().center
        else graph.getBounds(isRotationNode, includeBends = false, includeLabels = false).center
    }

    fun rotateNode(graph:IGraph, node:INode, renderContext: IRenderContext) {
        rotateOrientation(node, RotationDirection.CLOCKWISE)
        rotateNode(graph, node, node.center.toPointD(), RotationDirection.CLOCKWISE, { false }, { false }, renderContext )
    }

    private fun rotateNode(graph: IGraph, node:INode, rotationPoint:PointD, direction:RotationDirection, hasFixedAspectRatio:NodePredicate, isFixedGroup:NodePredicate, renderContext: IRenderContext?) {
        val delta = if(direction == RotationDirection.CLOCKWISE)
            (node.center - rotationPoint).let { PointD(-it.y, it.x) - it }
        else
            (node.center - rotationPoint).let { PointD(it.y, -it.x) - it }

        if(hasFixedAspectRatio(node)) {
            graph.setNodeLayout(node, node.layout.translate(delta))
            node.ports.forEach { port ->
                val newPortLocation = node.layout.convertToRatioPoint(port.location).let {
                    val newRatio = if (direction == RotationDirection.CLOCKWISE) PointD(1.0-it.y, it.x)
                    else PointD(it.y, 1.0-it.x)
                    node.layout.convertFromRatioPoint(newRatio)
                }
                graph.setPortLocation(port, newPortLocation)
            }
        }
        else {
            val portLocationMap = node.ports.map { port ->
                val portDelta = if (direction == RotationDirection.CLOCKWISE)
                    (port.location - rotationPoint).let { PointD(-it.y, it.x) - it }
                else
                    (port.location - rotationPoint).let { PointD(it.y, -it.x) - it }
                port to portDelta + port.location
            }
            val labelRatioPointMap = node.labels.map { label ->
                val ratioPoint = node.layout.convertToRatioPoint(label.layout.center)
                val newRatioPoint = when(direction) {
                    RotationDirection.CLOCKWISE -> PointD(1.0-ratioPoint.y, ratioPoint.x)
                    RotationDirection.COUNTER_CLOCKWISE -> PointD(ratioPoint.y, 1.0-ratioPoint.x)
                }
                label to newRatioPoint
            }

            graph.setNodeLayout(node, RectD.fromCenter(node.center + delta, node.layout.toSizeD().swap()))

            portLocationMap.forEach { (port, location) ->
                graph.setPortLocation(port, location)
            }

            //force all style features to be up-to-date
            node.style.renderer.getVisualCreator(node, node.style).createVisual(renderContext)


            labelRatioPointMap.forEach { (label, newRatioPoint) ->
                if(node.type == SbgnType.TAG && label.type == SbgnType.NAME_LABEL) {
                    (label.layoutParameter.model as? ILabelModelParameterFinder)?.let { finder ->
                        val newCenter = node.layout.convertFromRatioPoint(newRatioPoint)
                        val newParam = finder.findBestParameter(label, RectD.fromCenter(newCenter, label.layout.toSizeD()))
                        graph.setLabelLayoutParameter(label, newParam)
                    }
                }
            }
        }

        if(graph.isGroupNode(node)) {
            if (isFixedGroup(node)) {
                graph.groupingSupport.getDescendants(node).forEach { descendant ->
                    graph.setNodeLayout(descendant, descendant.layout.translate(delta))
                }
            }
            else {
                rotate(graph, node, rotationPoint, direction, { true }, hasFixedAspectRatio, isFixedGroup, renderContext)
            }
        }
    }

    private fun rotate(graph: IGraph, root:INode?, rotationPoint:PointD, direction:RotationDirection, isRotationNode:NodePredicate, hasFixedAspectRatio:NodePredicate, isFixedGroup:NodePredicate, renderContext:IRenderContext?) {
        graph.getChildren(root).forEach { node ->
            if(isRotationNode(node)) {
                rotateNode(graph, node, rotationPoint, direction, hasFixedAspectRatio, isFixedGroup, renderContext)
                graph.outEdgesAt(node).forEach { edge ->
                    if(isRotationNode(edge.targetNode) && edge.bends.any()) {
                        edge.bends.toList().forEach { bend ->
                            val bendDelta = if(direction == RotationDirection.CLOCKWISE)
                                (bend.location - rotationPoint).let { PointD(-it.y, it.x) - it }
                            else
                                (bend.location - rotationPoint).let { PointD(it.y, -it.x) - it }
                            graph.beginEdit(bend).use {
                                graph.setBendLocation(bend, bend.location + bendDelta)
                            }
                        }
                    }
                }
            }
        }
    }
}
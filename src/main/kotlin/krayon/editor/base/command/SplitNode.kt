/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.IPort
import krayon.editor.base.util.convertFromRatioPoint
import krayon.editor.base.util.convertToRatioPoint

object SplitNode : ApplicationCommand("SPLIT_NODE") {
    override fun canExecute(param: Any?): Boolean {
        val nodes = getNodes(param)
        return nodes.count() == 1 &&
                graph.edgesAt(nodes.first()).any { graphComponent.selection.isSelected(it) } &&
                graph.edgesAt(nodes.first()).any { !graphComponent.selection.isSelected(it) }
    }

    override fun execute(param: Any?) {
        getUniqueNode(param)?.let { node ->
            val edges = graph.edgesAt(node).filter { graphComponent.selection.isSelected(it) }
            graphComponent.selection.selectedNodes.clear()
            graphComponent.selection.setSelected(node, true)
            InteractiveDuplicate.execute(param)  //TODO
            val newNode = getUniqueNode(null)!!
            node.ports.forEach { port ->
                graph.edgesAt(port).filter { edges.contains(it) }.forEach { edge ->
                    if(edge.sourcePort == port) {
                        val newPort = determineCorrespondingPort(node,edge.sourcePort, newNode)
                        graph.setEdgePorts(edge,newPort, edge.targetPort)
                    }
                    else {
                        val newPort = determineCorrespondingPort(node, edge.targetPort, newNode)
                        graph.setEdgePorts(edge, edge.sourcePort, newPort)
                    }
                }
            }
        }
    }

    private fun createPort(node: INode, port: IPort, newNode:INode):IPort {
        val ratioPoint = node.layout.convertToRatioPoint(port.location)
        val newPoint = newNode.layout.convertFromRatioPoint(ratioPoint)
        return graph.addPort(newNode, newPoint)
    }

    private fun determineCorrespondingPort(node: INode, port: IPort, newNode:INode):IPort {
        var bestPort:IPort? = null
        var closestDist = Double.MAX_VALUE
        val point = node.layout.convertToRatioPoint(port.location)
        for (newPort in newNode.ports) {
            if(newPort.tag == port.tag) {
                val newPoint = newNode.layout.convertToRatioPoint(newPort.location)
                if(point.distanceTo(newPoint) < closestDist) {
                    bestPort = newPort
                    closestDist = point.distanceTo(newPoint)
                }
            }
        }
        return bestPort ?: createPort(node, port, newNode)
    }

}
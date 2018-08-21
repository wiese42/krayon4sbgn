/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.model

import com.yworks.yfiles.graph.IEdge
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.IPort

interface IModelConstraintManager<T> {

    fun getPreferredTargetType(graph: IGraph, source: INode, sourcePort: IPort?, arcType: T): T
    fun getPreferredSourceType(graph: IGraph, target: INode, targetPort: IPort?, arcType: T): T

    fun getEdgeCreationHints(graph: IGraph, source: INode, sourcePort: IPort?): List<IEdgeCreationHint<T>>
    fun getEdgeConversionHints(graph: IGraph, edge: IEdge): List<IEdgeCreationHint<T>>

    fun isNodeAcceptingPort(nodeType: T, portType: T): Boolean
    fun isEdgeAcceptingLabel(edge: IEdge, labelType: T): Boolean
    fun isNodeAcceptingLabel(node: INode, labelType: T): Boolean
    fun getNodeConversionTypes(graph: IGraph, node: INode): List<T>
    fun isValidTarget(graph: IGraph, source: INode, edgeType: T, target: INode, targetType: T, targetPort: IPort?): Boolean
    fun isValidSource(graph: IGraph, target: INode, edgeType: T, source: INode, sourceType: T, sourcePort: IPort?): Boolean
    fun isValidEdgeConversion(graph: IGraph, edge: IEdge, type: T): Boolean
    fun isMergeable(aGraph: IGraph, aNode: INode, targetGraph: IGraph, targetNode: INode): Boolean
    fun isMergeablePort(aGraph: IGraph, aPort: IPort, targetGraph: IGraph, targetPort: IPort): Boolean
    fun isValidChild(graph: IGraph, groupNodeType: T, node: INode): Boolean
}

interface IEdgeCreationHint<T> {
    val type:T
    val reversed:Boolean
}
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
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.input.IHandle
import com.yworks.yfiles.view.input.IInputModeContext
import com.yworks.yfiles.view.input.INodeHitTester
import krayon.editor.base.style.HighlightNodesManager
import krayon.editor.base.ui.DelegatingReshapeHandle
import krayon.editor.base.util.*
import krayon.editor.sbgn.model.sbgnConstraintManager
import krayon.editor.sbgn.model.type

class ReparentingReshapeHandle(handle: IHandle, val node: INode) : DelegatingReshapeHandle(handle) {

    private fun determineNodeMembers(context: IInputModeContext):Set<INode> {
        val graph = context.graph
        val members = graph.nodes.toHashSet()
        members.toList().forEach { member ->
            if(member == node) members.remove(member)
            else if(member.layout.toRectD().containsRect(node.layout)) members.remove(member)
            else if(graph.isAncestor(node,member)) members.remove(member)
            else if(!node.style.renderer.getHitTestable(node, node.style).isHit(context,member.center)) members.remove(member)
        }
        members.toList().forEach { member ->
            if(graph.getAncestors(member).any { members.contains(it) }) members.remove(member)
            if(graph.getAncestors(member).any { anc -> anc != node && !members.contains(anc) && !graph.isAncestor(node,anc) }) members.remove(member)
        }
        members.toList().forEach { member ->
            if(!context.sbgnConstraintManager.isValidChild(graph, node.type, member)) {
                members.remove(member)
                if(graph.isGroupNode(member)) {
                    graph.groupingSupport.getDescendants(member).forEach { members.remove(it) }
                }
            }
        }

        members.toList().forEach { member ->
            if(!context.sbgnConstraintManager.isValidChild(graph, node.type, member)) {
                members.remove(member)
                if(graph.isGroupNode(member)) {
                    graph.groupingSupport.getDescendants(member).forEach { members.remove(it) }
                }
            }
        }
        return members
    }

    override fun handleMove(context: IInputModeContext, p1: PointD?, p2: PointD?) {
        val highlightNodesSupport = context.graphComponent.lookup(HighlightNodesManager::class.java)
        super.handleMove(context, p1, p2)
        val members = determineNodeMembers(context)
        context.graph.nodes.forEach {
            if(members.contains(it)) {
                if(!highlightNodesSupport.isHighlighted(it)) {
                    highlightNodesSupport.addHighlight(it, "<member>")
                }
            }
            else if(highlightNodesSupport.isHighlighted(it)) highlightNodesSupport.removeHighlight(it)
        }
    }

    private fun getNewParent(context: IInputModeContext, child: INode): INode? {
        val hitTester = context.lookup(INodeHitTester::class.java)
        return hitTester.enumerateHits(context, child.center).firstOrNull { groupNode ->
            groupNode != child && context.graph.isGroupNode(groupNode) && context.sbgnConstraintManager.isValidChild(context.graph, groupNode.type, child)
        }
    }

    override fun dragFinished(context: IInputModeContext, p1: PointD, p2: PointD) {
        val highlightNodesSupport = context.graphComponent.lookup(HighlightNodesManager::class.java)
        super.dragFinished(context, p1, p2)
        highlightNodesSupport.clearHighlights()
        val members = determineNodeMembers(context)
        context.graph.nodes.forEach {
            if(context.graph.getParent(it) != node && members.contains(it)) {
                context.graph.setParent(it, node)
            }
            else if(context.graph.getParent(it) == node && !members.contains(it)) {
                val newParent = getNewParent(context, it)
                context.graph.setParent(it, newParent)
            }
        }
    }
}
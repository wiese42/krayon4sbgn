/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.IPoint
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.IModelItem
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.ICanvasObjectGroup
import com.yworks.yfiles.view.ModifierKeys
import com.yworks.yfiles.view.input.IInputModeContext
import com.yworks.yfiles.view.input.INodeHitTester
import krayon.editor.base.style.HighlightNodesManager
import krayon.editor.base.util.*
import krayon.editor.sbgn.model.SbgnMergeManager
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.sbgnConstraintManager
import krayon.editor.sbgn.model.type

class DragNodesManager {

    private var draggedNodeSet:  Set<INode>? = null
    private var draggedNodeSetGraph:  IGraph? = null
    private lateinit var draggedNodeOffset: IPoint
    private var activeParents = mutableSetOf<INode>()

    private val groupNodeIndex = HashMap<INode,Int>()
    private var traverseIndex:Int = 0

    fun startDrag(context:IInputModeContext, affectedGraph: IGraph, affectedItems: Iterable<IModelItem>, affectedNodeOffset: IPoint = PointD.ORIGIN) {
        cleanup(context)
        this.draggedNodeSetGraph = affectedGraph
        draggedNodeSet = affectedItems.mapNotNull { it as? INode }.toSet()
        this.draggedNodeOffset = affectedNodeOffset
        prepareGroupNodeIndices(context)
    }

    fun onDrag(context: IInputModeContext, location: PointD) {
        val highlightNodesSupport = context.graphComponent.lookup(HighlightNodesManager::class.java)
        highlightNodesSupport.clearHighlights()

        updateActiveParents(context, draggedNodeSetGraph!!, draggedNodeSet!!, draggedNodeOffset)


        val mergePair = SbgnMergeManager.findMergePair(context, draggedNodeSetGraph!!, draggedNodeSet!!, location, draggedNodeOffset)
        //println("location=$location  mergePairFound=${mergePair != null}")

        val isAdaptionPair = mergePair != null && draggedNodeSetGraph!!.degree(mergePair.first) == 0
        val useActionAlternative = context.graphComponent.lastMouse2DEvent.modifiers.contains(ModifierKeys.SHIFT)
        val hasActionAlternative = isAdaptionPair && activeParents.any { it == mergePair!!.second }


        //println("mergePair=$mergePair  hasAlt=$hasActionAlternative  useAlt=$useActionAlternative  isAdaptionPair=$isAdaptionPair")

        if(mergePair != null && (!hasActionAlternative || useActionAlternative)) {
            val text = if(isAdaptionPair) "adapt me!" else "connect me!"
            highlightNodesSupport.addHighlight(mergePair.second,text)
            activeParents.remove(mergePair.second)
        }
        if(activeParents.isNotEmpty()) {
            activeParents.forEach { highlightNodesSupport.addHighlight(it, "<group>") }
        }
    }

    fun onDrop(context: IInputModeContext, droppedItems:Iterable<IModelItem>, location: PointD, size: SizeD? = null, nameLabelText:String? = null) {
        val highlightNodesSupport = context.graphComponent.lookup(HighlightNodesManager::class.java)
        val droppedNodeSet = droppedItems.mapNotNull { it as? INode }.toSet()
        highlightNodesSupport.clearHighlights()
        with(context.graph) {

            val mergePair = SbgnMergeManager.findMergePair(context, this, droppedNodeSet, location)
            val isAdaptionPair = mergePair != null && context.graph.degree(mergePair.first) == 0
            val useActionAlternative = context.graphComponent.lastMouse2DEvent.modifiers.contains(ModifierKeys.SHIFT)
            val hasActionAlternative = isAdaptionPair && activeParents.any { it == mergePair!!.second }

            //println("activeParent=${activeParents.size}  droppedNodeSet=${droppedNodeSet.size})")

            if(mergePair == null || activeParents.isNotEmpty()) {
                droppedNodeSet.forEach { node ->
                    if(getParent(node) == null || !droppedNodeSet.contains(getParent(node))) {
                        val newParent = getParentAtMouseLocation(context, this, droppedNodeSet, node.center)
                        if(newParent == null || activeParents.contains(newParent)) {
                            setParent(node, newParent)
                        }
                    }
                }
            }

            if(mergePair != null && (!hasActionAlternative || useActionAlternative)) {
                val (aNode, hitNode) = mergePair
                if (degree(aNode) == 0) {
                    val newSize = size ?: aNode.layout.toSizeD()

                    SbgnMergeManager.mergeNodeFeatures(context, this, node = hitNode, templateNode = aNode, newSize = newSize, newLabelText = nameLabelText)
                    remove(aNode)
                } else {
                    //val delta = hitNode.center - aNode.center
                    val delta = SbgnMergeManager.getEdgeTransferDelta(context.graph, aNode, hitNode)

                    SbgnMergeManager.transferEdgesToNode(context, this, transferSource = aNode, transferTarget = hitNode)

                    @Suppress("UNCHECKED_CAST")
                    context.graph.translate(delta, droppedItems as Iterable<INode>)
                    SbgnMergeManager.removeTransferSourceAfterEdgeTransfer(this, aNode)

                }
            }
        }
        cleanup(context)
    }

    fun cleanup(context:IInputModeContext) {
        val highlightNodesSupport = context.graphComponent.lookup(HighlightNodesManager::class.java)
        highlightNodesSupport.clearHighlights()
        draggedNodeSet = null
        groupNodeIndex.clear()
    }

    private fun getParentAtMouseLocation(context: IInputModeContext, affectedNodesGraph:IGraph, affectedNodeSet:Set<INode>, location: PointD): INode? {
        val hitTester = context.lookup(INodeHitTester::class.java)
        return hitTester.enumerateHits(context, location).firstOrNull { groupNode ->
            context.graph.isGroupNode(groupNode) && !affectedNodeSet.contains(groupNode) &&
                    affectedNodeSet.all { context.sbgnConstraintManager.isValidChild(affectedNodesGraph, groupNode.type, it) }
        }
    }

    private fun updateActiveParents(context: IInputModeContext, affectedNodesGraph:IGraph, affectedNodeSet:Set<INode>, affectedNodesOffset: IPoint) {
        activeParents.clear()

        val affectedBounds = affectedNodesGraph.getBounds({ node -> affectedNodeSet.contains(node)}, false, false).translate(affectedNodesOffset.toPointD())
        val unaffectedNodes = context.graph.nodes.filter { node ->
            !affectedNodeSet.contains(node) && affectedBounds.intersects(node.layout.toRectD())
        }

        if(unaffectedNodes.isEmpty()) return

        affectedNodeSet.forEach { aNode ->
            val aPoint = aNode.center + affectedNodesOffset.toPointD()

            unaffectedNodes
                .filter { groupNode ->
                    groupNode.layout.contains(aPoint) &&
                            (!affectedNodesGraph.isGroupNode(aNode) || !context.graph.isAncestor(groupNode, aNode)) &&
                            context.graph.isGroupNode(groupNode) && !affectedNodeSet.contains(groupNode) &&
                            affectedNodeSet.all { context.sbgnConstraintManager.isValidChild(affectedNodesGraph, groupNode.type, it) }
                }
                .maxBy {
                    groupNodeIndex.getOrDefault(it,0)
                }
                ?.let {
                    activeParents.add(it)
                }
        }
    }

    private fun traverseCanvasTree(canvasGroup:ICanvasObjectGroup) {
        for(iter in canvasGroup) {
            if(iter.userObject is INode) {
                val node = iter.userObject as INode
                if(node.type.isComplex() || node.type == SbgnType.COMPARTMENT) {
                    groupNodeIndex[node] = traverseIndex++
                }
            }
            if(iter is ICanvasObjectGroup) {
                traverseCanvasTree(iter)
            }
        }
    }

    private fun prepareGroupNodeIndices(context:IInputModeContext) {
        traverseIndex = 0
        groupNodeIndex.clear()
        traverseCanvasTree(context.graphComponent.graphModelManager.contentGroup)
    }
}


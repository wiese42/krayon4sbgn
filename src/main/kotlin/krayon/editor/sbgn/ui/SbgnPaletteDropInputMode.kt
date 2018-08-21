/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.view.input.CreateEdgeInputMode
import krayon.editor.base.command.ApplicationCommand
import krayon.editor.base.command.CommandScope
import krayon.editor.base.model.IModelItemFeature
import krayon.editor.base.ui.GraphPaletteDropInputMode
import krayon.editor.base.util.beginEdit
import krayon.editor.base.util.ceim
import krayon.editor.base.util.convertToRatioPoint
import krayon.editor.sbgn.command.ToggleCloneMarker
import krayon.editor.sbgn.command.ToggleMultimer
import krayon.editor.sbgn.layout.MirrorTransformation
import krayon.editor.sbgn.layout.RotateTransformation
import krayon.editor.sbgn.model.*
import krayon.editor.sbgn.style.SbgnBuilder
import java.awt.dnd.DropTargetDragEvent

class SbgnPaletteDropInputMode(paletteGraph: IGraph) : GraphPaletteDropInputMode(paletteGraph) {

    private val nodeDropSupport = DragNodesManager()
    private val constraintManager get() = inputModeContext.sbgnConstraintManager
            
    init {
        setIsValidParentPredicate { it == null } //handle group nodes as DropActions
        isDropTargetHighlightEnabled = false //handle own highlights
    }

    override fun initializeDropTarget(event: DropTargetDragEvent) {
        super.initializeDropTarget(event)
        val itemFromDraggedNode = getItemFromDraggedNode()
        if(itemFromDraggedNode is INode) {
            nodeDropSupport.startDrag(localContext, transferGraph, listOf(itemFromDraggedNode), affectedNodeOffset)
        }
        else if(itemFromDraggedNode is IGraph) {
            nodeDropSupport.startDrag(localContext, itemFromDraggedNode, itemFromDraggedNode.nodes, affectedNodeOffset)
        }
    }

    override fun onDraggedOver(event: DropTargetDragEvent) {
        super.onDraggedOver(event)
        if(getItemFromDraggedNode().let { it is INode || it is IGraph}) {
            nodeDropSupport.onDrag(localContext, mousePosition.toPointD())
        }
    }

    override fun isGraphCreateDropTarget(graph: IGraph, dropTarget: IModelItem?, draggedGraph: IGraph): Boolean {
        return false //handle own reparenting
    }

    override fun isAcceptingLabel(edge: IEdge, paletteLabel: ILabel) = constraintManager.isEdgeAcceptingLabel(edge, paletteLabel.type)
    override fun isAcceptingLabel(node: INode, paletteLabel: ILabel) = constraintManager.isNodeAcceptingLabel(node, paletteLabel.type)
    override fun isAcceptingPort(node: INode, palettePort: IPort): Boolean = constraintManager.isNodeAcceptingPort(node.type, palettePort.type)

    override fun isAcceptingFeature(node: INode, feature: IModelItemFeature): Boolean {
        //TODO: move to constraintManager
        return when(feature.type) {
            SbgnType.CLONE_MARKER ->  node.type.canCarryCloneMarker()
            SbgnType.MULTIMER -> node.type.isMultimer() || node.type.canBeMultimer()
            else -> false
        }
    }

    override fun isAcceptingEdge(graph: IGraph, node: INode, port: IPort?, paletteEdge: IEdge): Boolean {
        return constraintManager.getEdgeCreationHints(graph, node, port).any { it.type == paletteEdge.type }
    }

    override fun isNodeCreationDropTarget(graph: IGraph, dropTarget: IModelItem?):Boolean {
        return dropTarget == null //handle group nodes as NodeActionDropTarget
    }

    override fun isNodeActionDropTarget(graph: IGraph, dropTarget: IModelItem?, paletteNode: INode): Boolean {
        return dropTarget is INode && constraintManager.getNodeConversionTypes(graph, dropTarget).any { it == paletteNode.type }
    }

    override fun isEdgeActionDropTarget(graph: IGraph, dropTarget: IModelItem?, paletteEdge: IEdge): Boolean {
        return if(dropTarget is IEdge) {
            dropTarget.type != paletteEdge.type && constraintManager.isValidEdgeConversion(graph, dropTarget, paletteEdge.type)
        }
        else false
    }

    override fun handleEdgeActionDrop(graph: IGraph, paletteEdge: IEdge, dropTarget: IModelItem?) {
        (dropTarget as? IEdge)?.let { edge ->
            graph.beginEdit("Convert Edge").use { _ ->
                val hint = constraintManager.getEdgeConversionHints(graph, edge).find { it.type == paletteEdge.type }
                if(hint != null) {
                    edge.type = paletteEdge.type
                    if(hint.reversed) graph.reverse(edge)
                    SbgnBuilder.configure(graph, edge)
                }
                else {
                    println("cannot convert edge. no valid conversion hints found.")
                }
            }
        }
    }

    override fun handleGraphDrop(graph: IGraph, paletteGraph: IGraph, layout: RectD) {
        super.handleGraphDrop(graph, paletteGraph, layout)
        nodeDropSupport.onDrop(localContext, graphComponent.selection.selectedNodes, layout.center)
    }

    override fun handleNodeActionDrop(graph: IGraph, dropTarget: IModelItem?, paletteNode: INode, layout: RectD) {
        //println("handleNodeActionDrop")
        val newNode = handleNodeCreationDrop(graph, paletteNode, null, layout)
        nodeDropSupport.onDrop(localContext, listOf(newNode), layout.center, layout.toSizeD())
    }

    override fun handleNodeFeatureDrop(graph: IGraph, paletteFeature: IModelItemFeature, dropTarget: IModelItem?) {
        //println("handleNodeFeatureDrop")
        if(paletteFeature.type == SbgnType.CLONE_MARKER)
            ToggleCloneMarker.execute(dropTarget as INode)
        else if(paletteFeature.type == SbgnType.MULTIMER) {
            ToggleMultimer.execute(dropTarget as INode)
        }
    }
    override fun onStartEdgeCreation(ceim: CreateEdgeInputMode, node: INode, paletteEdge: IEdge) {
        //println("SbgnPaletteComponent:onStartEdgeCreation")
        super.onStartEdgeCreation(ceim, node, paletteEdge)
        val hints = constraintManager.getEdgeCreationHints(ceim.graph, node, ceim.sourcePortCandidate?.port)
        val index = hints.indexOfFirst {  it.type == paletteEdge.type  }
        if(index >= 0 ) {
            val hint = hints[index]
            if (hint.reversed) SbgnBuilder.reverseEdgeStyle(ceim.dummyEdge)
            (ceim as? SbgnCreateEdgeInputMode)?.apply {
                edgeCreationHints = hints
                currentHint = hint
                if(currentHint == null) {
                    println()
                }
                nextHintIndex = 0
            }
        }
        else {
            println("trouble. cannot find hint for type: ${paletteEdge.type}")
        }
    }



    private var originalLayout: RectD? = null
    private var snapNode: INode? = null

    override fun initializeSnapContext(event: DropTargetDragEvent) {
        (getItemFromDraggedNode() as? INode)?.let { paletteNode ->
            SbgnBuilder.getPrevalentSize(localContext.graph, paletteNode.type, paletteNode.orientation)?.let { prevalentSize ->
                snapNode = paletteNode
                originalLayout = paletteNode.layout.toRectD()
                transferGraph.setNodeLayout(paletteNode, RectD.fromCenter(originalLayout!!.center, prevalentSize))
            }
        }
        super.initializeSnapContext(event)
    }

    override fun cleanup() {
        super.cleanup()
        if(snapNode != null) {
            transferGraph.setNodeLayout(snapNode, originalLayout)
            snapNode = null
            originalLayout = null
        }
    }

    override fun handleEdgeLabelDrop(graph: IGraph, paletteLabel: ILabel) {
        super.handleEdgeLabelDrop(graph, paletteLabel)
        graphComponent.selection.selectedLabels.firstOrNull()?.let { label ->
            label.owner.graphStyle?.let {
                SbgnBuilder.styleManager.applyStyle(it, graph, label)
            }
        }
    }

    override fun handleNodeLabelDrop(graph: IGraph, paletteLabel: ILabel) {
        super.handleNodeLabelDrop(graph, paletteLabel)
        graphComponent.selection.selectedLabels.firstOrNull()?.let { label ->
            label.owner.graphStyle?.let {
                SbgnBuilder.styleManager.applyStyle(it,graph,label)
            }
            if(label.type == SbgnType.CALLOUT_LABEL) {
                val center = label.layoutParameter.model.getGeometry(label,label.layoutParameter).center
                val node = label.owner as INode
                val calloutPoint = node.layout.convertToRatioPoint(center)
                label.setSbgnProperty(SbgnPropertyKey.CALLOUT_POINT, calloutPoint)
                val param = SbgnBuilder.getPreferredLabelParamForCalloutPoint(calloutPoint)

                graph.setLabelLayoutParameter(label, param)
            }
        }
    }

    fun updatePreviewLabels(graph:IGraph) {
        graph.nodes.firstOrNull()?.let { node ->
            previewGraph.nodes.firstOrNull()?.let { previewNode ->
                val previewLabels = previewNode.labels.toList()
                node.labels.forEachIndexed { index, label ->
                    val previewLabel = previewLabels[index]
                    previewGraph.setLabelLayoutParameter(previewLabel, label.layoutParameter)
                }
            }
        }
    }

    private abstract class DragItemCommand(id:String):ApplicationCommand(id,CommandScope.DRAG_ITEM) {
        override fun canExecute(param: Any?) = true
        override fun execute(param: Any?) {
            (graphComponent.ceim.mutexOwner as? SbgnPaletteDropInputMode)?.let {
                executeDragItemCommand(it, param)
                //necessary rotation and mirror commands that involve tag nodes
                it.updatePreview(it.transferGraph, it.mousePosition.toPointD())
                it.updatePreviewLabels(it.transferGraph)
            }
        }
        abstract fun executeDragItemCommand(inputMode:SbgnPaletteDropInputMode, param:Any?)
    }

    companion object {
        val RotateClockwise:ApplicationCommand = object:DragItemCommand("ROTATE_CLOCKWISE") {
            override fun executeDragItemCommand(inputMode: SbgnPaletteDropInputMode, param: Any?) {
                (inputMode.getItemFromDraggedNode() as? IGraph)?.let { paletteGraph ->
                    RotateTransformation.rotate(paletteGraph, paletteGraph.nodes, RotateTransformation.RotationDirection.CLOCKWISE,
                            graphComponent.createRenderContext())
                }
                (inputMode.getItemFromDraggedNode() as? INode)?.let { node ->
                    RotateTransformation.rotate(inputMode.transferGraph, listOf(node), RotateTransformation.RotationDirection.CLOCKWISE,
                            graphComponent.createRenderContext())
                }
            }
        }

        val MirrorHorizontally:ApplicationCommand = object:DragItemCommand("MIRROR_HORIZONTALLY") {
            override fun executeDragItemCommand(inputMode: SbgnPaletteDropInputMode, param: Any?) {
                (inputMode.getItemFromDraggedNode() as? IGraph)?.let { paletteGraph ->
                    MirrorTransformation.mirror(paletteGraph, paletteGraph.nodes, MirrorTransformation.MirrorAxis.HORIZONTAL)
                }
                (inputMode.getItemFromDraggedNode() as? INode)?.let { node ->
                    MirrorTransformation.mirror(inputMode.transferGraph, listOf(node), MirrorTransformation.MirrorAxis.HORIZONTAL)
                }
            }
        }

        val MirrorVertically:ApplicationCommand = object:DragItemCommand("MIRROR_VERTICALLY") {
            override fun executeDragItemCommand(inputMode: SbgnPaletteDropInputMode, param: Any?) {
                (inputMode.getItemFromDraggedNode() as? IGraph)?.let { paletteGraph ->
                    MirrorTransformation.mirror(paletteGraph, paletteGraph.nodes, MirrorTransformation.MirrorAxis.VERTICAL)
                }
                (inputMode.getItemFromDraggedNode() as? INode)?.let { node ->
                    MirrorTransformation.mirror(inputMode.transferGraph, listOf(node), MirrorTransformation.MirrorAxis.VERTICAL)
                }
            }
        }
    }
}
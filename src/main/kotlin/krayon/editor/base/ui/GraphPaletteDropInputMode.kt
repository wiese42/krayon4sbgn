/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.IPoint
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameter
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterFinder
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterProvider
import com.yworks.yfiles.graph.portlocationmodels.IPortLocationModelParameter
import com.yworks.yfiles.graph.styles.ILabelStyle
import com.yworks.yfiles.graph.styles.INodeStyle
import com.yworks.yfiles.graph.styles.IPortStyle
import com.yworks.yfiles.utils.ICloneable
import com.yworks.yfiles.utils.IEventListener
import com.yworks.yfiles.view.GraphComponent
import com.yworks.yfiles.view.input.*
import krayon.editor.base.command.CommandManager
import krayon.editor.base.command.CommandScope
import krayon.editor.base.model.IModelItemFeature
import krayon.editor.base.util.findBestParameter
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

open class GraphPaletteDropInputMode(private val paletteGraph: IGraph) : NodeDropInputMode() {
    private var edgeGraph: IGraph? = null
    private var hitNode: INode? = null
    private var hitEdge: IEdge? = null

    private var highlightPortsSupport = HighlightPortsSupport()

    /**
     * This graph holds the transfer data elements.
     */
    val transferGraph:IGraph = DefaultGraph()

    private var originalTransferData:Any? = null
    private var mappedTransferData:Any? = null

    /** minimum time in ms between commands that are triggered via the keyboard during dnd */
    var keyEventResolution = 500L

    protected lateinit var localContext:IInputModeContext
    protected val graphComponent
        get() = localContext.canvasComponent as GraphComponent


    private var dragLeftListener = IEventListener<DropTargetEventArgs<DropTargetEvent>> { _, _ ->
        validDropHitTestable = IHitTestable.ALWAYS
        highlightPortsSupport.deinstallPortHighlights()
    }

    /**
     * for unclear reasons we receive incomplete keyboard events. Therefore we process all we can get
     * and dispatch as soon as  keyEventResolution time has passed since last dispatch.
     */
    private var keyListener = object:KeyListener {
        var lastKeyEventTime = 0L

        override fun keyTyped(e: KeyEvent) {
            processKey(e)
        }

        override fun keyPressed(e: KeyEvent) {
            processKey(e)
        }

        override fun keyReleased(e: KeyEvent) {
            processKey(e)
        }

        fun processKey(e:KeyEvent) {
            val now = System.currentTimeMillis()
            if(now - lastKeyEventTime > keyEventResolution) {
                lastKeyEventTime = now
                val keyStroke = KeyStroke.getKeyStroke(e.keyCode, e.modifiers)
                CommandManager.getCommand(keyStroke, CommandScope.DRAG_ITEM)?.let {
                    CommandManager.execute(it, null, CommandManager.InvocationMethod.VIA_KEYBOARD)
                }
            }
        }
    }

    /**
     * return a copy of the Transferable data
     */
    override fun getTransferData(transferable: Transferable, dataFlavor: DataFlavor): Any? {
        val data = super.getTransferData(transferable, dataFlavor)
        if(data != originalTransferData) {
            transferGraph.clear()
            val paletteNode = data as INode
            val copyFilter = { item:IModelItem ->
                item == paletteNode || item is IPort && item.owner == paletteNode || item is ILabel && item.owner == paletteNode
            }
            GraphCopier().copy(paletteGraph, copyFilter, transferGraph, PointD.ORIGIN) { _, copiedItem ->
                if (copiedItem.tag is IGraph) {
                    val innerGraph = copiedItem.tag as IGraph
                    val origInnerGraphTag = innerGraph.tag
                    //println("origInnerGraphTag=$origInnerGraphTag")
                    val newInnerGraph = DefaultGraph().apply {
                        tag = origInnerGraphTag
                    }
                    GraphCopier().copy(copiedItem.tag as IGraph, { _ -> true }, newInnerGraph, PointD.ORIGIN, { innerOrigItem, innerCopiedItem ->
                        if(innerOrigItem == origInnerGraphTag) {
                            newInnerGraph.tag = innerCopiedItem
                        }
                        else if (innerCopiedItem.tag is ICloneable) {
                            innerCopiedItem.tag = (innerCopiedItem.tag as ICloneable).clone()
                        }
                    })
                    //println("assign newInnerGraph")
                    copiedItem.tag = newInnerGraph
                }
                else if (copiedItem.tag is ICloneable) {
                    copiedItem.tag = (copiedItem.tag as ICloneable).clone()
                }
            }
            mappedTransferData = transferGraph.nodes.first()
            originalTransferData = data
        }
        return mappedTransferData
    }

    override fun onDragEntered(event: DropTargetDragEvent) {
        super.onDragEntered(event)
        val node = dropData as INode
        validDropHitTestable = IHitTestable.ALWAYS
        if(node.tag is IGraph) {
            val innerGraph = node.tag as IGraph
            when {
                innerGraph.tag is IEdge -> validDropHitTestable = onNodeOrEdgeHitTestable //NodeHitTestable
                innerGraph.tag is ILabel -> validDropHitTestable = onEdgeHitTestable
                innerGraph.tag is IModelItemFeature -> validDropHitTestable = onNodeHitTestable
            }
        } else if(node.tag is ILabel || node.tag is IPort) {
            validDropHitTestable = onNodeHitTestable
        }

        graphComponent.requestFocus()
        graphComponent.addKeyListener(keyListener)
    }

    private fun removeKeyListeners() {
        graphComponent.removeKeyListener(keyListener)
    }

    override fun onDragDropped(p0: DropTargetDropEvent?) {
        removeKeyListeners()
        clearTransferState()
        super.onDragDropped(p0)
    }

    override fun onDragExited(p0: DropTargetEvent?) {
        removeKeyListeners()
        clearTransferState()
        super.onDragExited(p0)
    }

    private fun clearTransferState() {
        originalTransferData = null
        mappedTransferData = null
    }

    val affectedNodeOffset = object: IPoint {
        override fun getX() = draggedItem?.let { mousePosition.x - it.layout.center.x + it.layout.x} ?: 0.0
        override fun getY() = draggedItem?.let { mousePosition.y - it.layout.center.y + it.layout.y} ?: 0.0
    }

    private val onNodeHitTestable = IHitTestable { context, location ->
        //println("isHit")
        hitNode = localContext.lookup(INodeHitTester::class.java)?.enumerateHits(localContext, location)?.firstOrNull()
        val draggedEdge = getItemFromDraggedNode() as? IEdge
        if(draggedEdge != null) {
            highlightPortsSupport.deinstallPortHighlights()
            if(hitNode != null) {
                highlightPortsSupport.installPortHighlights(hitNode!!, location,
                        spcValidator = { isAcceptingEdge(context.graph, hitNode!!, it.port, draggedEdge) })
                if(highlightPortsSupport.focusedPortCandidate == null) {
                    return@IHitTestable false
                }
            }
        }
        if(hitNode != null) {
            val port = (draggedItem?.tag as? IPort)
            if(port != null && !isAcceptingPort(hitNode as INode, port)) return@IHitTestable false
            val label = (draggedItem?.tag as? ILabel)
            if(label != null && !isAcceptingLabel(hitNode as INode, label)) return@IHitTestable false
            val feature = getItemFromDraggedNode() as? IModelItemFeature
            if(feature != null && !isAcceptingFeature(hitNode as INode, feature)) return@IHitTestable false
        }
        hitNode != null
    }


    private val onEdgeHitTestable = IHitTestable { context, location ->
        hitEdge = context.lookup(IEdgeHitTester::class.java)?.enumerateHits(context, location)?.firstOrNull()
        if(hitEdge != null) {
            val draggedItem = getItemFromDraggedNode() as? IModelItem
            when (draggedItem) {
                is IEdge -> isEdgeActionDropTarget(graphComponent.graph, hitEdge, draggedItem)
                is ILabel -> isAcceptingLabel(hitEdge!!, draggedItem)
                else -> false
            }
        }
        else false
    }

    private val onNodeOrEdgeHitTestable = IHitTestable { context, location ->
        if(onNodeHitTestable.isHit(context, location)) true else onEdgeHitTestable.isHit(context, location)
    }

    init {
        setIsGroupNodePredicate{ node ->
            //was paletteGraph
            transferGraph.isGroupNode(node)
        }
    }

    protected fun getItemFromDraggedNode():Any? {
        val item = draggedItem
        if(item is INode) {
            val graph = item.tag as? IGraph
            if(graph != null) return graph.tag as? IEdge ?: graph.tag as? ILabel ?: graph.tag as? IModelItemFeature ?: graph
            return item.tag as? ILabel ?: item.tag as? IPort ?: item
        }
        else return item
    }

    override fun getDropTarget(location: PointD): IModelItem? {
        return if(onNodeOrEdgeHitTestable.isHit(localContext, location)) {
            val draggedNode = getItemFromDraggedNode() as? INode
            val draggedGraph = getItemFromDraggedNode() as? IGraph
            val hitItem = hitNode ?: hitEdge
            if(hitItem != null) {
                if(draggedNode != null && (isNodeCreationDropTarget(localContext.graph, hitItem) || isNodeActionDropTarget(localContext.graph, hitItem, draggedNode))) hitItem
                else if(draggedGraph != null && isGraphCreateDropTarget(localContext.graph, hitItem, draggedGraph)) hitItem
                else null
            }
            else null
        }
        else null
    }

    protected open fun isGraphCreateDropTarget(graph:IGraph, dropTarget: IModelItem?, draggedGraph:IGraph):Boolean {
        return dropTarget is INode && graph.isGroupNode(dropTarget)
    }

    protected open fun isNodeCreationDropTarget(graph: IGraph, dropTarget: IModelItem?):Boolean {
        return dropTarget == null || dropTarget is INode && graph.isGroupNode(dropTarget)
    }

    protected open fun isNodeActionDropTarget(graph: IGraph, dropTarget: IModelItem?, paletteNode: INode):Boolean = false
    protected open fun isEdgeActionDropTarget(graph: IGraph, dropTarget: IModelItem?, paletteEdge: IEdge):Boolean = false

    protected open fun isAcceptingLabel(edge: IEdge, paletteLabel: ILabel) = true
    protected open fun isAcceptingLabel(node: INode, paletteLabel: ILabel) = true
    protected open fun isAcceptingPort(node: INode, palettePort: IPort) = true
    protected open fun isAcceptingEdge(graph: IGraph, node: INode, port: IPort?, paletteEdge: IEdge) = true
    protected open fun isAcceptingFeature(node: INode, feature: IModelItemFeature) = false

    private fun handleEdgeDrop(graph: IGraph, paletteEdge: IEdge) {
        if (hitNode != null) {
            val focusedPortCandidate = highlightPortsSupport.focusedPortCandidate
            if (focusedPortCandidate != null) {
                val ceim = (graphComponent.inputMode as GraphEditorInputMode).createEdgeInputMode
                val startNodeForEdge = hitNode!! //tricky. will be set to null before callback triggers
                var startAction: IEventListener<InputModeEventArgs>? = null
                startAction = IEventListener { _, _ ->
                    onStartEdgeCreation(ceim, startNodeForEdge, paletteEdge)
                    ceim.removeGestureStartedListener(startAction)
                }
                ceim.addGestureStartedListener(startAction)

                validDropHitTestable = IHitTestable.ALWAYS
                edgeGraph = null
                hitNode = null

                SwingUtilities.invokeLater {
                    highlightPortsSupport.deinstallPortHighlights()
                    ceim.doStartEdgeCreation(focusedPortCandidate)
                }
            }
        }
        else if (hitEdge != null) { //apply edge style to hitEdge
            handleEdgeActionDrop(graph, paletteEdge, hitEdge)
        }
    }

    protected open fun handleEdgeLabelDrop(graph: IGraph, paletteLabel: ILabel) {
        if(hitEdge != null) {
            val newLabel = graph.addLabel(hitEdge, paletteLabel.text, paletteLabel.layoutParameter)
            val clonedStyle = paletteLabel.style.clone() as ILabelStyle
            newLabel.tag = if (paletteLabel.tag is ICloneable) (paletteLabel.tag as ICloneable).clone() else paletteLabel.tag

            graph.setStyle(newLabel, clonedStyle)
            graph.setLabelLayoutParameter(newLabel, getNearestLabelParameter(newLabel))
            graphComponent.selection.apply {
                clear()
                setSelected(newLabel, true)
            }
        }
    }

    protected open fun handleNodeLabelDrop(graph: IGraph, paletteLabel: ILabel) {
        if (hitNode != null) {
            val newLabel = graph.addLabel(hitNode, paletteLabel.text, paletteLabel.layoutParameter)
            val clonedStyle = paletteLabel.style.clone() as ILabelStyle
            graph.setStyle(newLabel, clonedStyle)
            (newLabel.layoutParameter.model as? ILabelModelParameterFinder)?.let { finder ->
                graph.adjustLabelPreferredSize(newLabel)
                graph.setLabelLayoutParameter(newLabel, finder.findBestParameter(newLabel, dropLocation))
            } ?: graph.setLabelLayoutParameter(newLabel, getNearestLabelParameter(newLabel))
            if (paletteLabel.tag is ICloneable) newLabel.tag = (paletteLabel.tag as ICloneable).clone()
            val selection = graphComponent.selection
            selection.clear()
            selection.setSelected(newLabel, true)
        }
    }

    protected open fun handleGraphDrop(graph: IGraph, paletteGraph: IGraph, layout: RectD) {
        val offset = layout.toPointD()
        val selection = graphComponent.selection
        selection.clear()
        GraphCopier().copy(paletteGraph, { _ -> true }, graph, offset, { _, copiedItem ->
            if (copiedItem is INode) {
                selection.setSelected(copiedItem, true)
            }
            if(copiedItem.tag is ICloneable) {
                copiedItem.tag = (copiedItem.tag as ICloneable).clone()
            }
        })
        if(dropTarget is INode && isGraphCreateDropTarget(graph, dropTarget, paletteGraph)) {
            graphComponent.selection.selectedNodes.filter { graph.getParent(it) == null }.forEach {
                graph.setParent(it, dropTarget as INode)
            }
        }
    }

    private fun handlePortDrop(graph: IGraph, palettePort: IPort) {
        val newPort = graph.addPort(hitNode, palettePort.locationParameter)
        val clonedStyle = palettePort.style.clone() as IPortStyle
        newPort.tag = if (palettePort.tag is ICloneable) (palettePort.tag as ICloneable).clone() else palettePort.tag
        graph.setStyle(newPort, clonedStyle)
        graph.setPortLocationParameter(newPort, getNearestPortLocation(newPort))
        val selection = graphComponent.selection
        selection.clear()
        selection.setSelected(newPort, true)
    }

    protected open fun handleNodeCreationDrop(graph: IGraph, paletteNode: INode, dropTarget: IModelItem?, layout: RectD): INode {
        val newNode = super.createNode(localContext, graph, paletteNode, dropTarget, layout)
        return newNode.apply {
            labels.forEach { label ->
                label.tag = if (label.tag is ICloneable) (label.tag as ICloneable).clone() else label.tag
            }
            localContext.graph.setStyle(this, style.clone() as INodeStyle)
            if (paletteNode.tag is Cloneable) tag = (paletteNode.tag as ICloneable).clone()

            ports.forEach {
                localContext.graph.setStyle(it, it.style.clone() as IPortStyle)
                if (it.tag is ICloneable) it.tag = (it.tag as ICloneable).clone()
            }
        }
    }

    protected open fun handleNodeActionDrop(graph: IGraph, dropTarget: IModelItem?, paletteNode: INode, layout: RectD) {
        //leave this to subclasses
    }

    protected open fun handleEdgeActionDrop(graph:IGraph, paletteEdge: IEdge, dropTarget: IModelItem?) {
        //leave this to subclasses
    }

    override fun createNode(context: IInputModeContext, graph: IGraph, node: INode, dropTarget: IModelItem?, layout: RectD): INode? {
        val item = getItemFromDraggedNode()
        //if(item != null) println("itemtype=" + item::class.java)
        when {
            item is IEdge -> {
                handleEdgeDrop(graph, item)
            }
            item is ILabel && item.owner is IEdge -> { //Palette Edge Label
                handleEdgeLabelDrop(graph, item)
            }
            item is IGraph -> { // Palette Graph
                handleGraphDrop(graph, item, layout)
            }
            item is ILabel && item.owner is INode ->
                handleNodeLabelDrop(graph, item)
            item is IPort -> {
                handlePortDrop(graph, item)
            }
            item is IModelItemFeature -> {
                handleNodeFeatureDrop(graph, item, hitNode)
            }
            item is INode && isNodeCreationDropTarget(graph, dropTarget) -> {
                return handleNodeCreationDrop(graph, item, dropTarget, layout)
            }
            item is INode && isNodeActionDropTarget(graph, dropTarget, item) -> {
                handleNodeActionDrop(graph, dropTarget, item, layout)
            }
        }
        return null
    }

    protected open fun handleNodeFeatureDrop(graph: IGraph, paletteFeature: IModelItemFeature, dropTarget: IModelItem?) {
        //leave this to subclasses
    }

    private fun getNearestLabelParameter(label: ILabel): ILabelModelParameter {
        //return label.layoutParameter
        var bestParam = label.layoutParameter
        val model = bestParam.model
        var bestDistance:Double = Double.MAX_VALUE

        if(model is ILabelModelParameterProvider) {
            val candidates = ArrayList<ILabelModelParameter>()
            candidates.add(bestParam)
            model.getParameters(label, model).forEach { c -> candidates.add(c) }
            val dropPoint = PointD(dropLocation.x, dropLocation.y)
            candidates.forEach { candidate ->
                val bounds = candidate.model.getGeometry(label,candidate).bounds
                val dist = bounds.center.distanceTo(dropPoint)
                if(dist < bestDistance) {
                    bestParam = candidate
                    bestDistance = dist
                }
            }
        }
        return bestParam
    }

    private fun getNearestPortLocation(port: IPort): IPortLocationModelParameter {
        return port.locationParameter.model.createParameter(port.owner, dropLocation)
    }

    override fun install(context: IInputModeContext, controller: ConcurrencyController) {
        super.install(context, controller)
        highlightPortsSupport.install(context)
        addDragLeftListener(dragLeftListener)
        this.localContext = IInputModeContext.create(this)
    }
    override fun uninstall(context: IInputModeContext) {
        super.uninstall(context)
        removeDragLeftListener(dragLeftListener)
        highlightPortsSupport.uninstall(context)
    }

    open fun onStartEdgeCreation(ceim: CreateEdgeInputMode, node: INode, paletteEdge: IEdge) {
        //println("PaletteDropMode:onStartEdgeCreation")
        ceim.dummyEdgeGraph.setStyle(ceim.dummyEdge, paletteEdge.style)
        ceim.dummyEdge.tag = if(paletteEdge.tag is ICloneable) (paletteEdge.tag as ICloneable).clone() else paletteEdge.tag
        paletteEdge.labels.forEach { label -> ceim.dummyEdgeGraph.addLabel(ceim.dummyEdge, label.text, label.layoutParameter, label.style, label.preferredSize, label.tag) }
    }
}
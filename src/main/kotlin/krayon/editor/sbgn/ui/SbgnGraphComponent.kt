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
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.labelmodels.FreeNodeLabelModel
import com.yworks.yfiles.graph.labelmodels.ILabelModelParameterFinder
import com.yworks.yfiles.graph.styles.Arrow
import com.yworks.yfiles.graph.styles.PolylineEdgeStyle
import com.yworks.yfiles.utils.IEnumerable
import com.yworks.yfiles.utils.ObservableCollection
import com.yworks.yfiles.view.*
import com.yworks.yfiles.view.input.*
import krayon.editor.base.model.IItemType
import krayon.editor.base.model.IModelConstraintManager
import krayon.editor.base.style.AnchoredNodeLabelModel
import krayon.editor.base.style.HighlightNodesManager
import krayon.editor.base.style.LabeledHighlightNodeVisualTemplate
import krayon.editor.base.ui.*
import krayon.editor.base.util.MOUSE_MIDDLE_PRESSED
import krayon.editor.base.util.ensureBefore
import krayon.editor.base.util.getMainCanvasObject
import krayon.editor.sbgn.command.EditLabel
import krayon.editor.sbgn.model.*
import krayon.editor.sbgn.style.SbgnMultimerStyle
import krayon.editor.sbgn.style.SbgnTerminalPortStyle
import java.awt.Color
import java.awt.Paint
import java.awt.geom.Ellipse2D
import java.util.function.Predicate

class SbgnGraphComponent : StyleableGraphComponent<SbgnType>(SbgnType.MAP) {

    val constraintManager = SbgnConstraintManager
    private val itemType = SbgnItemType()

    private val highlightNodesManager = object:HighlightNodesManager(this) {
        override fun createVisualTemplate(node: INode, tag: String): IVisualTemplate {
            return LabeledHighlightNodeVisualTemplate(highlightPen, tag, background)
        }
    }

    init {
        initialize()
    }

    private fun initialize() {

        enableEdgesBeforeNodesMode()

        val foldingView = FoldingManager().createFoldingView()
        graph = foldingView.graph

        foldingView.manager.masterGraph.isUndoEngineEnabled = true

        with(SbgnDecorations) {
                registerPortCandidateProvider(graph)
                registerStyleHandleProviders(this@SbgnGraphComponent)
                registerReconnectionPortCandidates(graph)
                registerGroupBoundsCalculator(graph)
                registerReshapeHandleProvider(graph)
                registerEdgePathClipper(graph)
                registerNodePositionHandler(graph)
                registerEdgePositionHandler(graph)
                registerStylableModelItemFeatures(graph)
                registerEdgePortHandleProvider(graph)
                //registerMarqueeTestableDecorator(graph)
                //registerHighlightIndicatorInstaller(it)
                //registerLabelPositionHandler(it)
                //registerLabelModelParameterFinders(it)
                //registerPortHighlights(it)
        }

        graph.nodeDefaults.portDefaults.isAutoCleanupEnabled = false
        graph.edgeDefaults.style = PolylineEdgeStyle().apply {
            targetArrow = Arrow.NONE
        }


        focusIndicatorManager.isEnabled = false

        putClientProperty(DefaultPortCandidateDescriptor.CANDIDATE_DRAWING_INVALID_FOCUSED_KEY, PortCandidateVisualTemplate(Color.RED, radius = 3.0))
        putClientProperty(DefaultPortCandidateDescriptor.CANDIDATE_DRAWING_INVALID_NON_FOCUSED_KEY, PortCandidateVisualTemplate(Color.RED, radius = 3.0))
        putClientProperty(DefaultPortCandidateDescriptor.CANDIDATE_DRAWING_VALID_FOCUSED_KEY, PortCandidateVisualTemplate(Color(161,192,87), radius = 3.5))
        putClientProperty(DefaultPortCandidateDescriptor.CANDIDATE_DRAWING_VALID_NON_FOCUSED_KEY, PortCandidateVisualTemplate(Color.ORANGE, radius = 3.0))
        //putClientProperty(RectangleIndicatorInstaller.HIGHLIGHT_TEMPLATE_KEY, HighlightNodeVisualTemplate())

        //adjust label layout param when label changes text and hence size
        graph.nodeDefaults.labelDefaults.isAutoAdjustingPreferredSizeEnabled = false
        graph.addLabelTextChangedListener { _,args ->
            val label = args.item
            graph.adjustLabelPreferredSize(label)
            val newParam = (label.layoutParameter.model as? ILabelModelParameterFinder)?.findBestParameter(label, label.layoutParameter.model,
                    label.layoutParameter.model.getGeometry(label, label.layoutParameter))
            if(newParam != null) graph.setLabelLayoutParameter(label, newParam)
        }


        graph.addLabelLayoutParameterChangedListener{ _, args ->
            if(args.item.layoutParameter?.model is FreeNodeLabelModel &&
                    args.oldValue?.model is AnchoredNodeLabelModel) {
                println("hey. got you!")
            }
        }
    }

    fun establishSbgnDrawingOrder() {
        graph.nodes.filter { it.type == SbgnType.COMPARTMENT }.reversed().forEach {
            graphModelManager.getMainCanvasObject(it).toBack()
        }
    }

    override fun createGraphClipboard(): GraphClipboard {
        val graphCopier = object:GraphCopier() {
            override fun copyTag(item: IModelItem, tag: Any?): Any? {
                return if (tag is SbgnData) tag.clone() as SbgnData
                else super.copyTag(item, tag)
            }
        }

        return object:GraphClipboard() {
            init {
                duplicateCopier = graphCopier
                fromClipboardCopier = graphCopier
                independentCopyItems = GraphItemTypes.NODE
                parentNodeDetection = ParentNodeDetectionModes.ROOT
            }

            /**
             * auto-include contents of selected complex node
             */
            fun Predicate<IModelItem>.withAutoInclusion():Predicate<IModelItem>? {
                val predicate = this
                return Predicate { item ->
                    if (!predicate.test(item)) {
                        val node = item as? INode ?: (item as? ILabel)?.owner as? INode
                        if(node != null) {
                            for (pathNode in graph.groupingSupport.getPathToRoot(node)) {
                                if (pathNode.type.isComplex() && predicate.test(pathNode)) return@Predicate true
                            }
                        }
                        false
                    } else true
                }
            }

            override fun duplicate(context: IInputModeContext?, graph: IGraph, predicate: Predicate<IModelItem>?,callback: IElementCopiedCallback?) {
                super.duplicate(context, graph, predicate?.withAutoInclusion(), callback)
            }

            override fun copy(graph: IGraph, predicate: Predicate<IModelItem>?) {
                super.copy(graph, predicate?.withAutoInclusion())
            }
        }
    }

    fun createEditorMode(): GraphEditorInputMode {

        val ceim = object:GraphEditorInputMode() {

            //trigger custom add label

            override fun onItemLeftDoubleClicked(args: ItemClickedEventArgs<IModelItem>) {
                super.onItemLeftDoubleClicked(args)
                if(!textEditorInputMode.isEditing) {
                    (args.item as? INode)?.let { EditLabel.execute(it) }
                }
            }

            //default order
            //clickHitTestOrder = arrayOf(GraphItemTypes.BEND, GraphItemTypes.EDGE_LABEL, GraphItemTypes.EDGE, GraphItemTypes.NODE, GraphItemTypes.NODE_LABEL, GraphItemTypes.PORT)

            //AuxLabel shall have higher selection priority than node
            override fun findItems(context: IInputModeContext, location: PointD, order: Array<GraphItemTypes>, test: Predicate<IModelItem>?): IEnumerable<IModelItem> {
                val hits = super.findItems(context, location, order, test)
                return when {
                    hits.any{ it.type.isAuxUnit()} -> super.findItems(context, location, order.ensureBefore(GraphItemTypes.NODE_LABEL, GraphItemTypes.NODE), test)
                    hits.any{ it.type == SbgnType.TERMINAL} -> super.findItems(context, location, order.ensureBefore(GraphItemTypes.PORT, GraphItemTypes.NODE), test)
                    else -> super.findItems(context, location, order, test)
                }
            }
        }

        return ceim.apply {

            //TerminalPorts
            doubleClickHitTestOrder = doubleClickHitTestOrder.ensureBefore(GraphItemTypes.PORT, GraphItemTypes.NODE)
            addItemLeftDoubleClickedListener(SbgnTerminalPortStyle.createEditLabelOnClickListener(this))
            addItemLeftDoubleClickedListener(SbgnMultimerStyle.addCloneMarkerLabelOnClickListener(this))

            isAddLabelAllowed = false
            isGroupingOperationsAllowed = true
            isCreateNodeAllowed = false
            isReverseEdgeAllowed = false

            // Enable snapping
            this.snapContext = GraphSnapContext().apply {
                //edgeToEdgeDistance = 10.0
                //nodeToEdgeDistance = 15.0
                //nodeToNodeDistance = 20.0
                snapDistance = 10.0
                //isSnappingOrthogonalMovementEnabled = true
                isSnappingBendsToSnapLinesEnabled = true
            }

            reparentNodeHandler = NoOpReparentNodeHandler()
            moveInputMode = SbgnMoveInputMode().apply { priority = moveInputMode.priority }

            marqueeSelectionInputMode.isEnabled = true

            isAutoRemovingEmptyLabelsEnabled = false

            itemHoverInputMode = MultiplexingNodeHoverInputMode().apply {
                priority = itemHoverInputMode.priority
                delegates += HighlightPortsDelegate()
                delegates += HighlightLockStateDelegate()
            }

            createBendInputMode = object : CreateBendInputMode() {}.apply {
                priority = createEdgeInputMode.priority + 1
            }

            createEdgeInputMode = SbgnCreateEdgeInputMode().apply {
                priority = createEdgeInputMode.priority
            }

            clickInputMode = object : ClickInputMode() {
                //crashes from time to time because of tweaked GraphModelManager
                override fun onClicked(args: ClickEventArgs) {
                    try { super.onClicked(args) } catch (ex: Exception) { }
                }
            }. apply {
                priority = clickInputMode.priority
            }

            handleInputMode = object : HandleInputMode() {}.apply {
                priority = handleInputMode.priority
            }

//            textEditorInputMode.textArea.addKeyListener(UnicodeShortcutKeyListener())
            textEditorInputMode = UnicodeTextEditorInputMode().apply {
                priority = textEditorInputMode.priority
            }

            isHidingLabelDuringEditingEnabled = false  //otherwise label borders etc won't show

            moveLabelInputMode.snapContext = LabelSnapContext().apply {
                snapDistance = 10.0
            }

            moveViewportInputMode.pressedRecognizer = MOUSE_MIDDLE_PRESSED.or(IEventRecognizer.MOUSE_LEFT_PRESSED.and(IEventRecognizer.SHIFT_PRESSED))
            moveViewportInputMode.draggedRecognizer = IEventRecognizer.MOUSE_DRAGGED
            moveViewportInputMode.releasedRecognizer = IEventRecognizer.MOUSE_RELEASED

            //commands.registerKeyBindings(keyboardInputMode)

            add(SbgnFileDropInputMode().apply { priority = nodeDropInputMode.priority - 1 })

            addDeletedItemListener { _, _ -> cleanupPorts() }
            addDeletingSelectionListener { _, _ -> cleanupPorts() }
            addEdgePortsChangedListener { _, _ -> cleanupPorts() }
        }
    }

    private fun cleanupPorts() {
        for (node in graph.nodes) {
            node.ports.filter { graph.degree(it) == 0 && it.type == SbgnType.NO_TYPE }.forEach(graph::remove)
        }
    }

    class PortCandidateVisualTemplate(val paint:Paint = Color.ORANGE, val pen:Pen = Pen.getBlack(), val radius:Double=3.0): IVisualTemplate {
        override fun createVisual(context: IRenderContext, bounds: RectD?, obj: Any?): IVisual? {
            return ShapeVisual(Ellipse2D.Double(-radius, -radius, 2*radius+1, 2*radius+1), pen, paint)
        }

        override fun updateVisual(context: IRenderContext, oldVisual: IVisual, bounds: RectD?, obj: Any?): IVisual? {
            return oldVisual
        }
    }

    //hacky hack. otherwise you can't start new edges on already connected border ports
    private fun enableEdgesBeforeNodesMode() {

        graphModelManager = object: GraphModelManager(this, this.contentGroup) {
            override fun <T : IModelItem?> typedHitElementsAt(p0: Class<T>?, p1: IInputModeContext?, p2: PointD?, p3: ICanvasObjectGroup?): IEnumerable<T> {
                val result = super.typedHitElementsAt(p0, p1, p2, p3)
                if (result.any { it is INode } && result.any { it is IEdge }) {
                    val list = ObservableCollection<T>()
                    var exceptionFound = false
                    result.forEach { item ->
                        if (item is IEdge && result.any { terminal -> terminal is INode && (item.sourceNode === terminal || item.targetNode === terminal) }) {
                            exceptionFound = true
                        }
                        else {
                            list.add(item)
                        }
                    }
                    if(exceptionFound) return list
                }
                return result
            }
        }
        graphModelManager.apply {
            isHierarchicEdgeNestingEnabled = false
            edgeGroup.above(nodeGroup)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <TLookup : Any?> lookup(clazz: Class<TLookup>?): TLookup {
        return when (clazz) {
            HighlightNodesManager::class.java -> highlightNodesManager as TLookup
            IModelConstraintManager::class.java -> constraintManager as TLookup
            IItemType::class.java -> itemType as TLookup
            IGraphComponentFactory::class.java -> object:IGraphComponentFactory {
                override fun createGraphComponent(): GraphComponent {
                    return SbgnGraphComponent()
                }
            } as TLookup
            else -> super.lookup(clazz)
        }
    }

    operator fun IEventRecognizer.plus(other: IEventRecognizer):IEventRecognizer = this.or(other)
    operator fun IEventRecognizer.times(other: IEventRecognizer):IEventRecognizer = this.and(other)
}




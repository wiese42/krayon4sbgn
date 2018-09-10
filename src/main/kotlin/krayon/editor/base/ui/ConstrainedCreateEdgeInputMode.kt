/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.styles.DefaultLabelStyle
import com.yworks.yfiles.utils.IEventListener
import com.yworks.yfiles.view.Mouse2DEventArgs
import com.yworks.yfiles.view.input.*
import krayon.editor.base.command.ApplicationCommand
import krayon.editor.base.command.CommandManager
import krayon.editor.base.command.CommandScope
import krayon.editor.base.model.IEdgeCreationHint
import krayon.editor.base.model.IItemType
import krayon.editor.base.model.IModelConstraintManager
import krayon.editor.base.util.geim
import krayon.editor.base.util.graphComponent
import krayon.editor.sbgn.style.SbgnBuilder
import java.awt.Color
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

open class ConstrainedCreateEdgeInputMode<T> : CreateEdgeInputMode() {

    var cycleEdgeTypeKeyCode = KeyEvent.VK_SPACE
    var edgeCreationHints:List<IEdgeCreationHint<T>> = emptyList()
    var currentHint: IEdgeCreationHint<T>? = null
    var nextHintIndex = 0
    @Suppress("UNCHECKED_CAST")
    val typeModel get() = inputModeContext.graphComponent.lookup(IItemType::class.java) as IItemType<T>
    @Suppress("UNCHECKED_CAST")
    val constraintManager get() = inputModeContext.graphComponent.lookup(IModelConstraintManager::class.java) as IModelConstraintManager<T>

    private var showInvalidPorts = true

    protected open fun configureTargetNode(target:INode, targetType:T, location:PointD, edgeHint: IEdgeCreationHint<T>) {
        typeModel.setType(target,targetType)
        graph.setNodeCenter(target, location)
    }

    protected open fun getPortForTargetNode(target: INode, edgeHint:IEdgeCreationHint<T>): IPort {
        return graph.addPort(target)
    }

    fun createEdgeWithNode(location:PointD) {
        if(isCreationInProgress) {
            val graphComponent = inputModeContext.graphComponent
            if (graphComponent.hitElementsAt(location).none { it is INode } &&  currentHint != null) {
                val edgeHint = currentHint!!  //heikel, heikel
                val source = sourcePortCandidate.owner as INode
                val graph = graphComponent.graph
                val targetType = if (edgeHint.reversed)
                    constraintManager.getPreferredSourceType(graph, source, sourcePortCandidate.port, edgeHint.type)
                else
                    constraintManager.getPreferredTargetType(graph, source, sourcePortCandidate.port, edgeHint.type)

                val target = graph.createNode()
                configureTargetNode(target, targetType, dummyEdge.targetPort.location, edgeHint)


                val sourcePort = sourcePortCandidate.port ?: sourcePortCandidate.createPort(inputModeContext)
                val targetPort = getPortForTargetNode(target, edgeHint)

                val edge = if (edgeHint.reversed) graph.createEdge(targetPort, sourcePort)
                else graph.createEdge(sourcePort, targetPort)

                (if(edgeHint.reversed) dummyEdge.bends.reversed() else dummyEdge.bends).forEach { graph.addBend(edge, it.location.toPointD()) }

                typeModel.setType(edge, edgeHint.type)
                SbgnBuilder.configure(graph, edge)
                cancel()
            }
        }
    }

    /**
     * handles edge + node creation when clicking on background
     */
    private var mouseClickedListener: IEventListener<Mouse2DEventArgs> = IEventListener { any, args ->
        if (isCreationInProgress && !createBendRecognizer.isRecognized(any, args) && !removeBendRecognizer.isRecognized(any,args)) {
           createEdgeWithNode(args.location)
        }
    }

    /**
     * key stroke to cycle edge types during edge creation
     */
    private val keyListener = object: KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if(isCreationInProgress) {
                val keyStroke = KeyStroke.getKeyStroke(e.keyCode, e.modifiers)
                CommandManager.getCommand(keyStroke, CommandScope.CREATE_EDGE)?.let {
                    CommandManager.execute(it, null, CommandManager.InvocationMethod.VIA_KEYBOARD)
                }
            }
        }
    }

    fun cycleDummyEdgeType(dummyEdge:IEdge? = null) {
        if(dummyEdge != null || isCreationInProgress) {
            val edge = dummyEdge ?: this.dummyEdge
            val hint = edgeCreationHints[nextHintIndex]
            currentHint = hint
            if(++nextHintIndex > edgeCreationHints.lastIndex) nextHintIndex = 0
            typeModel.setType(edge, hint.type)
            SbgnBuilder.configure(dummyEdgeGraph, edge)
            if(hint.reversed) SbgnBuilder.reverseEdgeStyle(edge)
            configureTypeLabelForDummyEdge(edge)
        }
    }

    private val typeLabelTag = "TYPE-LABEL-TAG"
    protected fun setTypeLabelToDummyEdge(dummyEdge:IEdge, text:String) {
        val typeLabel = dummyEdge.labels.find { it.tag == typeLabelTag } ?:
        dummyEdgeGraph.addLabel(dummyEdge, text).apply {
            tag = typeLabelTag
            dummyEdgeGraph.setStyle(this, DefaultLabelStyle().apply { backgroundPaint = Color.WHITE })
        }
        dummyEdgeGraph.setLabelText(typeLabel, text)
    }
    protected fun removeTypeLabelFromDummyEdge(dummyEdge:IEdge) {
        dummyEdge.labels.find { it.tag == typeLabelTag }?.let {
            dummyEdgeGraph.remove(it)
        }
    }

    protected open fun configureTypeLabelForDummyEdge(dummyEdge:IEdge) {
    }

    override fun onGestureStarting(p0: InputModeEventArgs?) {
        super.onGestureStarting(p0)
        val sourcePort = sourcePortCandidate?.port
        val source = sourcePortCandidate?.owner as? INode
        val edge = dummyEdge
        if(source != null) {
            edgeCreationHints = constraintManager.getEdgeCreationHints(graph, source, sourcePort)
            nextHintIndex = 0
            if(edgeCreationHints.isEmpty()) throw IllegalStateException("edge creation hints cannot be empty")
            cycleDummyEdgeType(edge)
        }
    }

    override fun createEdge(graph: IGraph, spc: IPortCandidate, tpc: IPortCandidate): IEdge {
        val edgeHint = currentHint!!

        removeTypeLabelFromDummyEdge(dummyEdge)

        return super.createEdge(graph, spc, tpc).apply {
            if(edgeHint.reversed) graph.reverse(this)
            typeModel.setType(this,edgeHint.type)
            SbgnBuilder.configure(graph, this)
        }
    }

    override fun onCanceled() {
        super.onCanceled()
        resetEdgeCreationHints()
    }

    override fun onEdgeCreated(p0: EdgeEventArgs?) {
        super.onEdgeCreated(p0)
        resetEdgeCreationHints()
    }

    fun doCreateEdge() {
        createEdge()
        cancel()
    }

    private fun resetEdgeCreationHints() {
        edgeCreationHints = emptyList()
        currentHint = null
        nextHintIndex = 0
    }

    init {
        //isCreateBendAllowed = false
        createBendRecognizer = IEventRecognizer.SHIFT_PRESSED.and(IEventRecognizer.MOUSE_CLICKED)
        removeBendRecognizer = IEventRecognizer.DELETE_PRESSED
        portCandidateResolutionRecognizer = IEventRecognizer.CTRL_PRESSED
        enforceBendCreationRecognizer = IEventRecognizer.NEVER
    }

    override fun install(context: IInputModeContext, p1: ConcurrencyController) {
        super.install(context, p1)
        context.canvasComponent.apply {
            addMouse2DClickedListener(mouseClickedListener)
            addKeyListener(keyListener)
        }
    }

    override fun uninstall(context: IInputModeContext) {
        context.canvasComponent.apply {
            removeMouse2DClickedListener(mouseClickedListener)
            removeKeyListener(keyListener)
        }
        super.uninstall(context)
    }

    /**
     * filter out candidates that violate that model
     */
    override fun getTargetPortCandidates(p0: PointD?, p1: Boolean): MutableIterable<IPortCandidate> {
        val constraintManager = constraintManager
        return if(currentHint != null) {
            val edgeHint = currentHint!!

            if(showInvalidPorts) {
                super.getTargetPortCandidates(p0, p1).map {
                    if(edgeHint.reversed && !constraintManager.isValidSource(graph, sourcePortCandidate.owner as INode, edgeHint.type, it.owner as INode, typeModel.getType(it.owner), it.port) ||
                    !edgeHint.reversed && !constraintManager.isValidTarget(graph, sourcePortCandidate.owner as INode, edgeHint.type, it.owner as INode, typeModel.getType(it.owner), it.port)) {
                        (if(it.port != null) DefaultPortCandidate(it.port) else DefaultPortCandidate(it.owner,it.locationParameter)).apply {
                            validity = PortCandidateValidity.INVALID
                        }
                    }
                    else it
                }.toMutableList()
            }
            else {
                super.getTargetPortCandidates(p0, p1).filter {
                    if(edgeHint.reversed)
                        constraintManager.isValidSource(graph, sourcePortCandidate.owner as INode, edgeHint.type, it.owner as INode, typeModel.getType(it.owner), it.port)
                    else
                        constraintManager.isValidTarget(graph, sourcePortCandidate.owner as INode, edgeHint.type, it.owner as INode, typeModel.getType(it.owner), it.port)
                }.toMutableList()
            }

        }
        else super.getTargetPortCandidates(p0, p1)
    }

    override fun getSourcePortCandidate(p0: PointD?): IPortCandidate? {
        val spc = super.getSourcePortCandidate(p0)
        return if (spc != null && constraintManager.getEdgeCreationHints(graph, spc.owner as INode, spc.port).isEmpty()) null else spc
    }

    companion object {
        val NextType = object : ApplicationCommand("NEXT_TYPE", CommandScope.CREATE_EDGE) {
            override fun canExecute(param: Any?) = true

            override fun execute(param: Any?) {
                (graphComponent.geim.createEdgeInputMode as? ConstrainedCreateEdgeInputMode<*>)?.let {
                    if (it.isCreationInProgress) {
                        it.cycleDummyEdgeType()
                        graphComponent.repaint()
                    }
                }
            }
        }

        val BeginEdgeCreation = object : ApplicationCommand("BEGIN_EDGE_CREATION") {
            override fun canExecute(param: Any?) = true

            override fun execute(param: Any?) {
                val location = graphComponent.lastMouse2DEvent.location
                with(graphComponent.geim.inputModeContext) {
                    val node = lookup(INodeHitTester::class.java)?.enumerateHits(this, location)?.firstOrNull()
                    if (node != null) {
                        val portCandidates = node.lookup(IPortCandidateProvider::class.java)?.getSourcePortCandidates(this)
                        if (portCandidates != null) {
                            val pc = HighlightPortsSupport.getClosestPortCandidate(node, location, portCandidates)
                            if (pc != null) {
                                graphComponent.geim.createEdgeInputMode.doStartEdgeCreation(pc)
                            }
                        }
                    }
                }
            }
        }

        val EndEdgeCreation = object:ApplicationCommand("END_EDGE_CREATION", CommandScope.CREATE_EDGE) {
            override fun canExecute(param: Any?): Boolean {
                return true
            }

            override fun execute(param: Any?) {
                val location = graphComponent.lastMouse2DEvent.location
                with(graphComponent.geim.createEdgeInputMode as ConstrainedCreateEdgeInputMode<*>) {
                    val node = graph.lookup(INodeHitTester::class.java)?.enumerateHits(inputModeContext, location)?.firstOrNull()
                    if (node != null) {
                        doCreateEdge()
                    }
                    else {
                        createEdgeWithNode(location)
                    }
                }
            }
        }
    }
}

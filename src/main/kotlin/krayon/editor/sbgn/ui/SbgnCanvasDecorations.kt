/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.*
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.portlocationmodels.FreeNodePortLocationModel
import com.yworks.yfiles.graph.styles.DefaultEdgePathCropper
import com.yworks.yfiles.graph.styles.IArrow
import com.yworks.yfiles.graph.styles.IEdgePathCropper
import com.yworks.yfiles.graph.styles.IShapeGeometry
import com.yworks.yfiles.utils.FlagsEnum
import com.yworks.yfiles.view.GraphComponent
import com.yworks.yfiles.view.ModifierKeys
import com.yworks.yfiles.view.input.*
import krayon.editor.base.model.IModelItemFeature
import krayon.editor.base.model.IModelItemFeatureProvider
import krayon.editor.base.model.SimpleFeature
import krayon.editor.base.ui.MoveDecendantsWithGroupPositionHandler
import krayon.editor.base.ui.ValidatingPortCandidateProvider
import krayon.editor.base.util.center
import krayon.editor.base.util.minus
import krayon.editor.sbgn.model.SbgnConstraintManager
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.CloneMarkerFeatureStyleable
import krayon.editor.sbgn.style.SbgnSourceAndSinkStyle
import java.awt.Cursor
import java.util.*
import kotlin.math.max


object SbgnDecorations {

    val cloneMarkerFeature = SimpleFeature(null).apply {
        type = SbgnType.CLONE_MARKER
        style = CloneMarkerFeatureStyleable()
    }

    val multimerFeature = SimpleFeature(null).apply {
        type = SbgnType.MULTIMER
    }


    fun registerStylableModelItemFeatures(graph: IGraph) {
        graph.decorator.nodeDecorator.getDecoratorFor(IModelItemFeatureProvider::class.java).setImplementation(
                object:IModelItemFeatureProvider {
                    override fun getFeatures(item: IModelItem): List<IModelItemFeature> {
                        val features = mutableListOf<IModelItemFeature>()
                        if(item.type.canCarryCloneMarker()) {
                            cloneMarkerFeature.owner = item
                            features.add(cloneMarkerFeature)
                        }
                        if(item.type.isMultimer() || item.type.canBeMultimer()) {
                            multimerFeature.owner = item
                            features.add(multimerFeature)
                        }
                        return features
                    }
                })
    }

    fun registerNodePositionHandler(graph:IGraph) {
        graph.decorator.nodeDecorator.positionHandlerDecorator.setFactory {
            return@setFactory MoveDecendantsWithGroupPositionHandler(it)
        }
    }

    /**
     * don't move unselected bends of selected edges
     */
    fun registerEdgePositionHandler(graph:IGraph) {
        graph.decorator.edgeDecorator.positionHandlerDecorator.setImplementation(null)
    }

    /**
     * unrestricted group node bounds
     */
    fun registerGroupBoundsCalculator(graph:IGraph) {
        graph.decorator.nodeDecorator.groupBoundsCalculatorDecorator.setImplementation(
                IGroupBoundsCalculator { _, _ -> RectD.EMPTY }
        )
    }

    /**
     * allow only 1:1 aspect ratio
     */
    fun registerReshapeHandleProvider(graph:IGraph) {
        val nodeDecorator = graph.decorator.nodeDecorator
        nodeDecorator.reshapeHandleProviderDecorator.setImplementationWrapper { node, delegateProvider: IReshapeHandleProvider ->
            if(graph.isGroupNode(node)) {
                return@setImplementationWrapper ReparentingReshapeHandlerProvider(delegateProvider, node)
            }
            // Obtain the tag from the node
            if (node.style is SbgnSourceAndSinkStyle)
                return@setImplementationWrapper CenteredAspectRatioHandleProvider(delegateProvider, node)
            else
                return@setImplementationWrapper MultiplexingReshapeHandleProvider(delegateProvider, node)
        }
    }

    class ReparentingReshapeHandlerProvider(private val delegateProvider: IReshapeHandleProvider, private val node: INode) : IReshapeHandleProvider {
        override fun getAvailableHandles(inputModeContext: IInputModeContext): HandlePositions? {
            return delegateProvider.getAvailableHandles(inputModeContext)
        }
        override fun getHandle(inputModeContext: IInputModeContext, position: HandlePositions): IHandle {
            return ReparentingReshapeHandle(delegateProvider.getHandle(inputModeContext, position), node)
        }
    }

    fun registerStyleHandleProviders(graphComponent: GraphComponent) {
        val labelDecorator = graphComponent.graph.decorator.labelDecorator
        labelDecorator.handleProviderDecorator.setImplementationWrapper { label, delegateProvider ->
            if (graphComponent.selection.isSelected(label)) {
                return@setImplementationWrapper when {
                    label.style is IHandleProvider -> label.style as IHandleProvider
                    else -> delegateProvider
                }
            } else return@setImplementationWrapper null
        }
    }


    fun registerEdgePathClipper(graph:IGraph) {
        graph.decorator.portDecorator.edgePathCropperDecorator.setImplementationWrapper { port, _ ->
            return@setImplementationWrapper if(port.type == SbgnType.INPUT_AND_OUTPUT || port.owner.type.isLogic()) inputOutputPortEdgePathCropper
            else auxLabelEdgePathCropper
        }
    }

    private val inputOutputPortEdgePathCropper = object:IEdgePathCropper {
        val defaultCropper = DefaultEdgePathCropper()
        override fun cropEdgePath(edge: IEdge, atSource: Boolean, arrow: IArrow, path: GeneralPath): GeneralPath {
            return defaultCropper.cropEdgePathAtArrow(atSource,arrow, path)
        }
    }

    private val auxLabelEdgePathCropper = object:DefaultEdgePathCropper() {
        override fun getIntersection(node: INode, nodeGeom: IShapeGeometry, edge: IEdge, inner: PointD, outer: PointD): PointD {
            val p = super.getIntersection(node, nodeGeom, edge, inner, outer)
            for (label in node.labels) {
                if(label.type.isAuxUnit()) {
                    val bounds = label.layout.bounds
                    bounds.findLineIntersection(p, outer)?.let { return it }
                }
            }
            return p
        }
    }

    fun registerPortCandidateProvider(graph:IGraph) {
        val nodeDecorator = graph.decorator.nodeDecorator
        nodeDecorator.portCandidateProviderDecorator.setFactory { node ->
            return@setFactory when {
                node.type.hasFixedPorts() -> {
                    if(node.type.isRegulableProcess()) {
                        fromExistingPortsAndNodeCenter(node)
                    }
                    else {
                        ValidatingPortCandidateProvider(IPortCandidateProvider.fromExistingPorts(node),
                        { spc -> when {
                            spc.port == null || SbgnConstraintManager.constraintLevel == SbgnConstraintManager.ConstraintLevel.NONE -> true
                            spc.owner.type == SbgnType.DISSOCIATION -> graph.inDegree(spc.port) < 1
                            spc.owner.type == SbgnType.ASSOCIATION -> graph.outDegree(spc.port) < 1
                            spc.owner.type == SbgnType.NOT -> graph.outDegree(spc.port) < 1 && graph.inDegree(spc.port) < 1
                            spc.owner.type.isLogic() -> graph.outDegree(spc.port) < 1
                            else -> true
                        } },
                        { true })
                    }
                }
                node.type == SbgnType.SOURCE_AND_SINK -> ValidatingPortCandidateProvider(fromExistingPortsAndNodeCenter(node),
                        { graph.degree(node) < 1}, {true})
                else -> {
                    IPortCandidateProvider.combine(
                        if(node.type == SbgnType.COMPARTMENT) IPortCandidateProvider.fromShapeGeometry(node, 0.5) else fromExistingPortsAndNodeCenter(node),
                        IPortCandidateProvider.fromCandidates(DefaultPortCandidate(node, FreeNodePortLocationModel.INSTANCE)))
                }
            }
        }
    }

    fun fromExistingPortsAndNodeCenter(node: INode):IPortCandidateProvider {
        return if (node.ports.any { (it.location-node.layout.center).vectorLength < 1.0 })
            IPortCandidateProvider.fromExistingPorts(node)
        else
            IPortCandidateProvider.combine(IPortCandidateProvider.fromNodeCenter(node), IPortCandidateProvider.fromExistingPorts(node))
    }

    fun fromExistingPortsAndNodeSides(node: INode):IPortCandidateProvider {
        return IPortCandidateProvider.combine(IPortCandidateProvider.fromShapeGeometry(node, 0.5), IPortCandidateProvider.fromExistingPorts(node))
    }

    /**
     * use Ctrl key (instead of Shift) to freely assign dynamic port
     */
    fun registerEdgePortHandleProvider(graph:IGraph) {
        graph.decorator.edgeDecorator.edgePortHandleProviderDecorator.setImplementationWrapper { edge, _ ->
            object:PortRelocationHandleProvider(graph, edge) {
                override fun createPortRelocationHandle(graph: IGraph?, edge: IEdge, isSourceEnd: Boolean): IHandle {
                    return PortRelocationHandle(graph,edge, isSourceEnd).apply {
                        portCandidateResolutionRecognizer = IEventRecognizer.CTRL_PRESSED
                    }
                }
            }
        }
    }

    fun registerReconnectionPortCandidates(graph: IGraph) {
        graph.decorator.edgeDecorator.edgeReconnectionPortCandidateProviderDecorator.setFactory { edge ->
            object:IEdgeReconnectionPortCandidateProvider {
                override fun getTargetPortCandidates(context: IInputModeContext): MutableIterable<IPortCandidate> {
                    with(edge.targetNode) {
                        return when {
                            type.hasFixedPorts() -> {
                                val result = mutableListOf<IPortCandidate>()
                                if (type.isRegulableProcess() && edge.type.isRegulation()) result += DefaultPortCandidate(edge.targetNode, FreeNodePortLocationModel.INSTANCE)
                                result += IPortCandidateProvider.fromExistingPorts(edge.targetNode).getTargetPortCandidates(context).filter {
                                    SbgnConstraintManager.isValidTarget(graph,edge.sourceNode, edge.type, edge.targetNode, edge.targetNode.type, it.port)
                                }
                                result
                            }
                            else -> {
                                mutableListOf<IPortCandidate>(DefaultPortCandidate(edge.targetNode, FreeNodePortLocationModel.INSTANCE)).apply {
                                    if(type == SbgnType.COMPARTMENT) {
                                        addAll(fromExistingPortsAndNodeSides(edge.targetNode).getTargetPortCandidates(context)) }
                                    else {
                                        addAll(fromExistingPortsAndNodeCenter(edge.targetNode).getTargetPortCandidates(context))
                                    }
                                }
                            }
                        }
                    }
                }

                override fun getSourcePortCandidates(context: IInputModeContext): MutableIterable<IPortCandidate> {
                    with(edge.sourceNode) {
                        return when {
                            type.hasFixedPorts() -> {
                                val result = mutableListOf<IPortCandidate>()
                                if (type.isRegulableProcess() && edge.type.isRegulation()) result += DefaultPortCandidate(edge.sourceNode, FreeNodePortLocationModel.INSTANCE)
                                result += IPortCandidateProvider.fromExistingPorts(edge.sourceNode).getTargetPortCandidates(context).filter {
                                    SbgnConstraintManager.isValidSource(graph,edge.targetNode, edge.type, edge.sourceNode, edge.sourceNode.type, it.port)
                                }
                                result
                            }
                            else -> {
                                mutableListOf<IPortCandidate>(DefaultPortCandidate(edge.sourceNode, FreeNodePortLocationModel.INSTANCE)).apply {
                                    if(type == SbgnType.COMPARTMENT) {
                                        addAll(fromExistingPortsAndNodeSides(edge.sourceNode).getSourcePortCandidates(context))
                                    }
                                    else {
                                        addAll(fromExistingPortsAndNodeCenter(edge.sourceNode).getSourcePortCandidates(context))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


class CenteredAspectRatioHandleProvider(private val delegateProvider: IReshapeHandleProvider, private val node: INode) : IReshapeHandleProvider {

    /**
     * Returns the available handles provided by the delegate provider
     * restricted to the ones in the four corners.
     */
    override fun getAvailableHandles(inputModeContext: IInputModeContext): HandlePositions? {
        // return only corner handles
        return FlagsEnum.and(
                delegateProvider.getAvailableHandles(inputModeContext), HandlePositions.NORTH_EAST)
    }

    /**
     * Returns a custom handle to maintains the aspect ratio of the node.
     */
    override fun getHandle(inputModeContext: IInputModeContext, position: HandlePositions): IHandle {
        return CenteredAspectRatioHandle(delegateProvider.getHandle(inputModeContext, position), position, node)
    }
}

/**
 * A handle that maintains the aspect ratio of the node.
 *
 *
 * Note that the simpler solution for this use case is subclassing [ConstrainedHandle],
 * however the interface is completely implemented for illustration, here.
 *
 */
private class CenteredAspectRatioHandle(private val handle: IHandle, private val position: HandlePositions, private val node: INode) : IHandle {
    private val layout: IRectangle = node.layout
    private var lastLocation: PointD? = null
    private var ratio: Double = 0.toDouble()
    private var originalBounds: RectD? = null
    private var newBounds: RectD? = null

    private val reshapeHandler: IReshapeHandler = node.lookup(IReshapeHandler::class.java)
    private var reshapeRectangleContext: ReshapeRectangleContext? = null
    private var reshapeSnapResultProvider: INodeReshapeSnapResultProvider? = null

    override fun getType(): HandleTypes {
        return handle.type
    }

    override fun getCursor(): Cursor {
        return handle.cursor
    }

    override fun getLocation(): IPoint {
        return node.layout.topRight
    }

    /**
     * Stores the initial location and aspect ratio for reference, and calls the base method.
     */
    override fun initializeDrag(inputModeContext: IInputModeContext) {
        initializeSnapping(inputModeContext)
        reshapeHandler.initializeReshape(inputModeContext)
        lastLocation = PointD(handle.location)
        originalBounds = RectD(layout)
        ratio = -layout.width / layout.height
        //for snapping
        reshapeRectangleContext = ReshapeRectangleContext(layout.toRectD(), SizeD.EMPTY, SizeD.INFINITE, RectD.EMPTY, position,
                PointD(-1.0, 1.0), PointD(1.0, -1.0), SizeD(2.0, -2.0))
    }

    /**
     * Constrains the movement to maintain the aspect ratio. This is done
     * by calculating the constrained location for the given new location,
     * and invoking the original handler with the constrained location.
     */
    override fun handleMove(inputModeContext: IInputModeContext, originalLocation: PointD, _newLocation: PointD) {
        var newLocation = _newLocation
        // For the given new location, the larger node side specifies the actual size change.
        var deltaDrag = PointD.subtract(newLocation, originalLocation)

        // manipulate the drag so that it respects the boundaries and enforces the ratio.
        if (Math.abs(ratio) > 1) {
            // the width is larger, so we take the north or south position as indicator for the dragY calculation
            // if the south handles are dragged, we have to add the value of the dragDeltaY to the original height
            // otherwise, we have to subtract the value.
            // calculate the dragX in respect to the dragY

            // the sign basically indicates from which side we are dragging and thus if
            // we have to add or subtract the drag delta y to the height
            val sign = (if (HandlePositions.SOUTH_EAST == position || HandlePositions.SOUTH_WEST == position) 1 else -1).toDouble()
            val newHeight = originalBounds!!.getHeight() + sign * (2.0 * deltaDrag.getX() / ratio)

            deltaDrag = if (newHeight > MIN_SIZE) {
                // if the new height is larger then the minimum size, set the deltaDragY to the deltaDragX with respect to the ratio.
                PointD(deltaDrag.getX(), deltaDrag.getX() / ratio)
            } else {
                // if the new height would fall below the minimum size, adjust the dragY so that the minimum size is satisfied and
                // then set the deltaDragX according to that value.
                val newDragY = Math.signum(deltaDrag.getX() / ratio) * (originalBounds!!.getHeight() - MIN_SIZE)
                PointD(newDragY * ratio, newDragY)
            }
        } else {
            // the height is larger, so we take the west or east position as indicator for the dragX calculation
            // if the west handles are dragged, we have to add the value of the dragDeltaX to the original width
            // otherwise, we have to subtract the value.
            // calculate the dragY in respect to the dragX

            // the sign basically indicates from which side we are dragging and thus if
            // we have to add or subtract the drag delta x to the width
            val sign = (if (HandlePositions.NORTH_EAST == position || HandlePositions.SOUTH_EAST == position) 1 else -1).toDouble()
            val newWidth = originalBounds!!.getWidth() + sign * (2.0 * deltaDrag.getY() * ratio)
            deltaDrag = if (newWidth > MIN_SIZE) {
                // if the new width is larger then the minimum size, set the deltaDragX to the deltaDragY with respect to the ratio.
                PointD(deltaDrag.getY() * ratio, deltaDrag.getY())
            } else {
                // if the new width would fall below the minimum size, adjust the dragX so that the minimum size is satisfied and
                // then set the deltaDragY according to that value.
                val newDragX = Math.signum(deltaDrag.getY() * ratio) * (originalBounds!!.getWidth() - MIN_SIZE)
                PointD(newDragX, newDragX / ratio)
            }
        }

        newLocation = PointD.add(originalLocation, deltaDrag)

        if (newLocation != lastLocation) {
            val newBounds = getNewBounds(deltaDrag)
            if (newBounds.width >= 0 && newBounds.height >= 0) {
                this.newBounds = newBounds
                reshapeHandler.handleReshape(inputModeContext, originalBounds, newBounds)
                lastLocation = newLocation
            }
        }
    }

    private fun getNewBounds(delta: PointD): RectD {
        var x = originalBounds!!.getX()
        var y = originalBounds!!.getY()
        var w = originalBounds!!.getWidth()
        var h = originalBounds!!.getHeight()

        w += delta.getX() * 2
        h -= delta.getY() * 2
        x -= delta.getX()
        y += delta.getY()

        return RectD(x, y, w, h)
    }

    override fun cancelDrag(inputModeContext: IInputModeContext, originalLocation: PointD) {
        finishSnapping(inputModeContext)
        reshapeHandler.cancelReshape(inputModeContext, originalBounds)
    }

    override fun dragFinished(inputModeContext: IInputModeContext, originalLocation: PointD, newLocation: PointD) {
        finishSnapping(inputModeContext)
        reshapeHandler.reshapeFinished(inputModeContext, originalBounds, newBounds)
    }

    internal var collect = { source:Any, args:CollectSnapResultsEventArgs ->
        if (reshapeSnapResultProvider != null && reshapeRectangleContext != null) {
            val graphSnapContext = source as GraphSnapContext
            val tmpResults = ArrayList<SnapResult>()
            val newArgs = CollectSnapResultsEventArgs(args.context,
                    originalBounds!!.topRight, location.toPointD(), 1.0, tmpResults)
            reshapeSnapResultProvider!!.collectSnapResults(graphSnapContext, newArgs, node, reshapeRectangleContext)
            for (snapResult in tmpResults) {
                args.addSnapResult(snapResult)
            }
        }
    }

    private fun initializeSnapping(inputModeContext: IInputModeContext) {
        reshapeRectangleContext = null
        val snapContext = inputModeContext.lookup(SnapContext::class.java)
        if (snapContext is GraphSnapContext && snapContext.isInitializing()) {
            snapContext.addItemToBeReshaped(node)
            reshapeSnapResultProvider = node.lookup(INodeReshapeSnapResultProvider::class.java)
            if (reshapeSnapResultProvider != null) {
                snapContext.addCollectSnapResultsListener(collect)
            }
        }
    }

    private fun finishSnapping(inputModeContext: IInputModeContext) {
        val snapContext = inputModeContext.lookup(SnapContext::class.java)
        if (snapContext is GraphSnapContext) {
            snapContext.removeCollectSnapResultsListener(collect)
            reshapeSnapResultProvider = null
        }
    }

    companion object {
        private const val MIN_SIZE = 5
    }
}


class WidthNoLessThanHeightConstraintHandle(handle:IHandle, val  position:HandlePositions, val node:INode) : ConstrainedHandle(handle) {
    private val origBounds = node.layout.toRectD()
    private var lastLocation:PointD? = null
    override fun constrainNewLocation(context: IInputModeContext, p1: PointD, p2: PointD): PointD {
        val newHeight = when(position) {
            HandlePositions.NORTH, HandlePositions.NORTH_WEST, HandlePositions.NORTH_EAST -> max(1.0, origBounds.maxY - p2.y)
            HandlePositions.SOUTH, HandlePositions.SOUTH_WEST, HandlePositions.SOUTH_EAST -> max(1.0, p2.y - origBounds.y)
            else -> node.layout.height
        }
        val newWidth = when(position) {
            HandlePositions.EAST, HandlePositions.SOUTH_EAST, HandlePositions.NORTH_EAST -> max(1.0, p2.x - origBounds.x)
            HandlePositions.WEST, HandlePositions.SOUTH_WEST, HandlePositions.NORTH_WEST -> max(1.0, origBounds.maxX - p2.x)
            else -> node.layout.width
        }
        lastLocation = if(newHeight > newWidth) lastLocation ?: p1 else p2
        return  lastLocation!!
    }
}

class MultiplexingReshapeHandleProvider(private val delegateProvider: IReshapeHandleProvider, private val node: INode) : IReshapeHandleProvider {

    override fun getAvailableHandles(inputModeContext: IInputModeContext): HandlePositions? {
        return if(node.type.isSimpleChemical()) HandlePositions.HORIZONTAL.or(HandlePositions.VERTICAL) else HandlePositions.BORDER
    }

    override fun getHandle(inputModeContext: IInputModeContext, position: HandlePositions): IHandle {
        return when {
            inputModeContext.canvasComponent.lastMouse2DEvent.modifiers.contains(ModifierKeys.SHIFT) ->
                CenteredReshapeHandle(delegateProvider.getHandle(inputModeContext, position), position, node)
            node.type.isSimpleChemical() ->
                WidthNoLessThanHeightConstraintHandle(delegateProvider.getHandle(inputModeContext, position), position, node)
            else ->
                delegateProvider.getHandle(inputModeContext, position)
        }
    }
}


/**
 * A handle that maintains resizes a node while keeping its center location
 */
private class CenteredReshapeHandle(private val handle: IHandle, private val position: HandlePositions, private val node: INode) : IHandle {

    private lateinit var originalBounds: RectD
    private var newBounds: RectD? = null

    private lateinit var center: PointD
    private lateinit var origHandleLocation:PointD

    private val reshapeHandler: IReshapeHandler = node.lookup(IReshapeHandler::class.java)
    private var reshapeRectangleContext: ReshapeRectangleContext? = null
    private var reshapeSnapResultProvider: INodeReshapeSnapResultProvider? = null

    override fun getType(): HandleTypes {
        return handle.type
    }

    override fun getCursor(): Cursor {
        return handle.cursor
    }

    override fun getLocation(): IPoint {
        return when(position) {
            HandlePositions.NORTH -> PointD(node.layout.center.x, node.layout.y)
            HandlePositions.NORTH_EAST -> node.layout.topRight
            HandlePositions.NORTH_WEST -> node.layout.topLeft
            HandlePositions.SOUTH -> PointD(node.layout.center.x, node.layout.maxY)
            HandlePositions.SOUTH_EAST -> node.layout.bottomRight
            HandlePositions.SOUTH_WEST -> node.layout.bottomLeft
            HandlePositions.EAST -> PointD(node.layout.maxX, node.layout.center.y)
            HandlePositions.WEST -> PointD(node.layout.x, node.layout.center.y)
            else -> node.layout.center
        }
    }

    /**
     * Stores the initial location and aspect ratio for reference, and calls the base method.
     */
    override fun initializeDrag(inputModeContext: IInputModeContext) {
        origHandleLocation = location.toPointD()
        initializeSnapping(inputModeContext)
        reshapeHandler.initializeReshape(inputModeContext)
        originalBounds = RectD(node.layout)
        newBounds = originalBounds
        center = node.center

        //ratio = -layout.width / layout.height
        //for snapping
        reshapeRectangleContext = ReshapeRectangleContext(node.layout.toRectD(), SizeD.EMPTY, SizeD.INFINITE, RectD.EMPTY, position,
                PointD(-1.0, 1.0), PointD(1.0, -1.0), SizeD(2.0, -2.0))
    }

    /**
     * Constrains the movement to maintain the aspect ratio. This is done
     * by calculating the constrained location for the given new location,
     * and invoking the original handler with the constrained location.
     */
    override fun handleMove(inputModeContext: IInputModeContext, originalLocation: PointD, newLocation: PointD) {
        val newHeight = when(position) {
            HandlePositions.NORTH, HandlePositions.NORTH_WEST, HandlePositions.NORTH_EAST -> max(MIN_SIZE, 2.0*(center.y - newLocation.y))
            HandlePositions.SOUTH, HandlePositions.SOUTH_WEST, HandlePositions.SOUTH_EAST -> max(MIN_SIZE, 2.0*(newLocation.y - center.y))
            else -> node.layout.height
        }
        val newWidth = when(position) {
            HandlePositions.EAST, HandlePositions.SOUTH_EAST, HandlePositions.NORTH_EAST -> max(MIN_SIZE, 2.0*(newLocation.x - center.x))
            HandlePositions.WEST, HandlePositions.SOUTH_WEST, HandlePositions.NORTH_WEST -> max(MIN_SIZE, 2.0*(center.x-newLocation.x))
            else -> node.layout.width
        }

        if(newWidth >= newHeight || !node.type.isSimpleChemical()) { //not perfect.
            newBounds = RectD.fromCenter(center, SizeD(newWidth, newHeight))
        }

        reshapeHandler.handleReshape(inputModeContext, originalBounds, newBounds)

    }

    override fun cancelDrag(inputModeContext: IInputModeContext, originalLocation: PointD) {
        finishSnapping(inputModeContext)
        reshapeHandler.cancelReshape(inputModeContext, originalBounds)
    }

    override fun dragFinished(inputModeContext: IInputModeContext, originalLocation: PointD, newLocation: PointD) {
        finishSnapping(inputModeContext)
        reshapeHandler.reshapeFinished(inputModeContext, originalBounds, newBounds)
    }

    internal var collect = { source:Any, args:CollectSnapResultsEventArgs ->
        if (reshapeSnapResultProvider != null && reshapeRectangleContext != null) {
            val graphSnapContext = source as GraphSnapContext
            val tmpResults = ArrayList<SnapResult>()
            val newArgs = CollectSnapResultsEventArgs(args.context,
                    origHandleLocation, location.toPointD(), 2.0, tmpResults)
            reshapeSnapResultProvider!!.collectSnapResults(graphSnapContext, newArgs, node, reshapeRectangleContext)
            for (snapResult in tmpResults) {
                args.addSnapResult(snapResult)
            }
        }
    }

    private fun initializeSnapping(inputModeContext: IInputModeContext) {
        reshapeRectangleContext = null
        val snapContext = inputModeContext.lookup(SnapContext::class.java)
        if (snapContext is GraphSnapContext && snapContext.isInitializing()) {
            snapContext.addItemToBeReshaped(node)
            reshapeSnapResultProvider = node.lookup(INodeReshapeSnapResultProvider::class.java)
            if (reshapeSnapResultProvider != null) {
                snapContext.addCollectSnapResultsListener(collect)
            }
        }
    }

    private fun finishSnapping(inputModeContext: IInputModeContext) {
        val snapContext = inputModeContext.lookup(SnapContext::class.java)
        if (snapContext is GraphSnapContext) {
            snapContext.removeCollectSnapResultsListener(collect)
            reshapeSnapResultProvider = null
        }
    }

    companion object {
        private const val MIN_SIZE = 5.0
    }
}

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
import com.yworks.yfiles.graph.DefaultGraph
import com.yworks.yfiles.graph.IModelItem
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.input.IInputModeContext
import com.yworks.yfiles.view.input.IPositionHandler
import com.yworks.yfiles.view.input.MoveInputMode
import krayon.editor.base.util.graphComponent
import krayon.editor.base.util.minus
import krayon.editor.base.util.plus

/**
 * moves the child elements of a group node along with it
 */
class MoveDecendantsWithGroupPositionHandler(val node: INode) : IPositionHandler {

    private fun getDefaultNodeHandler(node: INode) = DefaultGraph.getDefaultNodeLookup().lookup(node, IPositionHandler::class.java) as IPositionHandler

    private val delegateHandler = getDefaultNodeHandler(node)

    private val handlerMap = mutableMapOf<IModelItem, Pair<IPositionHandler, PointD>>()

    override fun getLocation(): IPoint {
        return delegateHandler.location
    }

    private fun initializeHandlerMap(context: IInputModeContext) {
        handlerMap.clear()
        handlerMap[node] = Pair(delegateHandler, node.layout.topLeft)
        if(context.parentInputMode is MoveInputMode && context.graph.isGroupNode(node)) {
            for (descendant in context.graph.groupingSupport.getDescendants(node)) {
                if(!context.graphComponent.selection.isSelected(descendant)) {
                    handlerMap[descendant] = Pair(getDefaultNodeHandler(descendant), descendant.layout.topLeft)
                }
            }
        }
        for (item in handlerMap.keys.toList()) {
            for (edge in context.graph.outEdgesAt(item as INode)) {
                if(handlerMap.containsKey(edge.targetNode)) {
                    handlerMap[edge] = Pair(DefaultGraph.getDefaultEdgeLookup().lookup(edge, IPositionHandler::class.java) as IPositionHandler, edge.sourcePort.location)
                }
            }
        }
    }

    override fun initializeDrag(context: IInputModeContext) {
        initializeHandlerMap(context)
        handlerMap.forEach { _, (handler, _) -> handler.initializeDrag(context) }
    }

    override fun handleMove(context: IInputModeContext, p1: PointD, p2: PointD) {
        handlerMap.forEach { _, (handler, pos) -> handler.handleMove(context, pos, pos + (p2 - p1)) }
    }

    override fun dragFinished(context: IInputModeContext, p1: PointD, p2: PointD) {
        handlerMap.forEach { _, (handler, pos) -> handler.dragFinished(context, pos, pos + (p2 - p1)) }
    }

    override fun cancelDrag(context: IInputModeContext, p: PointD) {
        handlerMap.forEach { _, (handler, pos) -> handler.cancelDrag(context, pos) }
    }

}
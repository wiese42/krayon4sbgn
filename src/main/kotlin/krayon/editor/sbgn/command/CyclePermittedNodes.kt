/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import com.yworks.yfiles.graph.INode
import krayon.editor.base.util.beginEdit
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.SbgnBuilder

object CyclePermittedNodes : SbgnCommand("CYCLE_PERMITTED_NODES") {

    private var lastCycleNode: INode? = null

    override fun execute(param:Any?) {
        val node = getUniqueNode(param)!!
        graph.beginEdit(id,listOf(node, node.tag)).use {
            val allowedTypes = sbgnGraphComponent.constraintManager.getNodeConversionTypes(graph,node)
            if(node != lastCycleNode) {
                lastCycleNode = node
                val nextType = allowedTypes.firstOrNull {node.type != allowedTypes }
                if(nextType != null) {
                    SbgnBuilder.assignType(graph, node, nextType)
                }
            }
            else {
                val typeIndex = allowedTypes.indexOf(node.type)
                if(typeIndex >= 0) {
                    val nextIndex = if(typeIndex == allowedTypes.lastIndex) 0 else typeIndex+1
                    SbgnBuilder.assignType(graph, node, allowedTypes[nextIndex])
                }
            }
        }
        graph.invalidateDisplays()
    }

    //requires single node
    override fun canExecute(param: Any?) = getUniqueNode(param) != null
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import com.yworks.yfiles.graph.IEdge
import com.yworks.yfiles.graph.ILabel
import com.yworks.yfiles.graph.IModelItem
import com.yworks.yfiles.graph.INode
import krayon.editor.base.util.convertToRatioPoint
import krayon.editor.sbgn.model.SbgnData
import krayon.editor.sbgn.model.type

object DumpTypeInfo : SbgnCommand("DUMP_TYPE_INFO") {
    override fun execute(param: Any?) {
        val item: IModelItem? = getUniqueNode(param) ?: getUniqueEdge(param) ?: getUniqueLabel(param)
        ?: getUniquePort(param)
        if (item is IModelItem) {
            println("type=" + item.type)
            (item.tag as? SbgnData)?.property?.forEach { (k, v) ->
                println("key=$k  value=$v")
            }
        }
        if (item is ILabel) {
            val bounds = item.layoutParameter.model.getGeometry(item, item.layoutParameter).bounds
            println("bounds=$bounds")
            println("layoutParameter=${item.layoutParameter}")
            (item.owner as? INode)?.let {
                println("centerRatioPoint=${it.layout.convertToRatioPoint(item.layout.center)}")
            }
        }
        if (item is INode) {
            println("layout=${item.layout}")
            sbgnGraphComponent.constraintManager.getNodeConversionTypes(graph, item).forEach {
                println("allowedNodeType=$it")
            }
            item.ports.forEach {
                println("port@${it.location}  type=${it.type}")
            }
            if (graph.isGroupNode(item))
                println("childCount=${graph.getChildren(item).size()}   parent=${graph.getParent(item)}")
        }
        if (item is IEdge) {
            item.bends.forEach {
                println("bend ${it.location}")
            }
            sbgnGraphComponent.constraintManager.getEdgeConversionHints(graph, item).forEach {
                println("possible conversions: $it")
            }
        }
    }
    override fun canExecute(param: Any?) = true
}
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
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.getNameLabel
import krayon.editor.sbgn.model.isClone
import krayon.editor.sbgn.model.type

object AutoAssignCloneMarkers : SbgnCommand("AUTO-ASSIGN_CLONE_MARKERS") {
    override fun canExecute(param: Any?) = true
    override fun execute(param: Any?) {
        graph.beginEdit(id, graph.nodes.map { it.tag }).use {
            graph.apply {
                val cloneSet = nodes.filter { it.isClone }.toHashSet()
                nodes.forEach { it.isClone = false }
                nodes.filter { it.type.canCarryCloneMarker() && getParent(it)?.type != SbgnType.COMPLEX }.
                        groupBy { node -> Triple(getParent(node), node.type, createNodeSignature(node)) }.
                        filter { g -> g.value.size > 1 }.
                        map { it -> it.value }.flatten().forEach { it.isClone = true }
                cloneSet.forEach { node ->
                    if(!node.isClone) { // remove label if clone marker was removed
                        node.labels.filter { it.type == SbgnType.CLONE_LABEL }.forEach { graph.remove(it) }
                    }
                }
                invalidateDisplays()
            }
        }
    }

    private fun createNodeSignature(node: INode):String {
        return  node.getNameLabel()?.text + '|' +
                node.labels.filter { label ->
                    (label.type == SbgnType.STATE_VARIABLE || (label.type == SbgnType.UNIT_OF_INFORMATION && label.text != "N:2")) && !label.text.isEmpty()
                }.map{ it.text }.sorted().joinToString("|") +
                if(node.type == SbgnType.COMPLEX) graph.getChildren(node).map { createNodeSignature(it) }.sorted() else ""
    }
}
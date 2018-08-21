/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import krayon.editor.base.util.addValueUndoEdit
import krayon.editor.base.util.beginEdit
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.isClone
import krayon.editor.sbgn.model.type

object ToggleCloneMarker : SbgnCommand("TOGGLE_CLONE_MARKER") {
    override fun execute(param: Any?) {
        graph.beginEdit(id).use {
            getNodes(param).let { nodes ->
                nodes.forEach { node ->
                    graph.addValueUndoEdit(id, node.isClone, !node.isClone, { node.isClone = it})
                    node.isClone = !node.isClone
                    if(!node.isClone) {
                        node.labels.filter{ it.type == SbgnType.CLONE_LABEL }.forEach {
                            graph.remove(it)
                        }
                    }
                }
            }
        }
        graph.invalidateDisplays()
    }
    override fun canExecute(param: Any?) =  selectedNodes.any { it.type.canCarryCloneMarker() }
}
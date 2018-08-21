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
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.SbgnBuilder

object ToggleMultimer : SbgnCommand("TOGGLE_MULTIMER") {
    override fun execute(param: Any?) {
        graph.beginEdit(id).use {
            getNodes(param).let { nodes ->
                nodes.forEach { node ->
                    val newType = when(node.type) {
                        SbgnType.SIMPLE_CHEMICAL -> SbgnType.SIMPLE_CHEMICAL_MULTIMER
                        SbgnType.MACROMOLECULE -> SbgnType.MACROMOLECULE_MULTIMER
                        SbgnType.NUCLEIC_ACID_FEATURE -> SbgnType.NUCLEIC_ACID_FEATURE_MULTIMER
                        SbgnType.COMPLEX -> SbgnType.COMPLEX_MULTIMER
                        SbgnType.SIMPLE_CHEMICAL_MULTIMER -> SbgnType.SIMPLE_CHEMICAL
                        SbgnType.MACROMOLECULE_MULTIMER -> SbgnType.MACROMOLECULE
                        SbgnType.NUCLEIC_ACID_FEATURE_MULTIMER -> SbgnType.NUCLEIC_ACID_FEATURE
                        SbgnType.COMPLEX_MULTIMER -> SbgnType.COMPLEX
                        else -> node.type
                    }
                    graph.addValueUndoEdit(id, node.type, newType, { node.type = it})
                    node.type = newType
                    if(node.type.isMultimer()) {
                        if(node.labels.none { it.type == SbgnType.UNIT_OF_INFORMATION && it.text.startsWith(("N:")) }) {
                            val label = graph.addLabel(node, "N:2")
                            graph.addValueUndoEdit(id, SbgnType.NO_TYPE, SbgnType.UNIT_OF_INFORMATION, { label.type = it})
                            label.type = SbgnType.UNIT_OF_INFORMATION
                            SbgnBuilder.configure(graph, label)
                        }
                    }
                    else {
                        node.labels.filter { it.type == SbgnType.UNIT_OF_INFORMATION && it.text.startsWith("N:") }.forEach(graph::remove)
                    }
                }
            }
        }
        graph.invalidateDisplays()
    }
    override fun canExecute(param: Any?) =  selectedNodes.any {
        it.type.isMultimer() || it.type.canBeMultimer()
    }
}
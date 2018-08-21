/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import krayon.editor.base.util.ceim
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.SbgnBuilder

object EditLabel : SbgnCommand("EDIT_LABEL") {

    override fun execute(param: Any?) {
        getUniqueNode(param)?.let { node ->
            if (sbgnGraphComponent.constraintManager.isNodeAcceptingLabel(node, SbgnType.NAME_LABEL) &&
                    node.labels.none { it.type == SbgnType.NAME_LABEL }) {
                SbgnBuilder.addNameLabel(graph, node)
            }
        }
        val label = getUniqueLabel(param) ?: getUniqueNode(param)?.labels?.firstOrNull { it.type == SbgnType.NAME_LABEL }
        if(label != null) graphComponent.ceim.editLabel(label)
    }

    override fun canExecute(param: Any?): Boolean {
        return (getUniqueLabel(param)
                ?: getUniqueNode(param)?.labels?.firstOrNull { it.type == SbgnType.NAME_LABEL }) != null
    }
}
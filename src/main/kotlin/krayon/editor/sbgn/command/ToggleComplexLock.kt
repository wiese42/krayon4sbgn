/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import krayon.editor.base.ui.MultiplexingNodeHoverInputMode
import krayon.editor.base.util.addValueUndoEdit
import krayon.editor.base.util.beginEdit
import krayon.editor.base.util.geim
import krayon.editor.sbgn.model.isLocked
import krayon.editor.sbgn.model.type

object ToggleComplexLock : SbgnCommand("TOGGLE_COMPLEX_LOCK") {
    override fun canExecute(param: Any?) = getNodes(param).any { it.type.isComplex() }
    override fun execute(param: Any?) {
        graph.beginEdit(id).use { _ ->
            getNodes(param).forEach { node ->
                if(node.type.isComplex()) {
                    graph.addValueUndoEdit(id, node.isLocked, !node.isLocked) { node.isLocked = it}
                    node.isLocked = !node.isLocked
                }
            }
        }
        (graphComponent.geim.itemHoverInputMode as? MultiplexingNodeHoverInputMode)?.updateHoverEffects()
        graph.invalidateDisplays()
    }
}

/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import krayon.editor.base.util.beginEdit
import krayon.editor.sbgn.layout.RotateTransformation

object RotateCounterClockwise: SbgnCommand("ROTATE_COUNTER_CLOCKWISE") {
    override fun canExecute(param: Any?) = selectedNodes.any()
    override fun execute(param: Any?) {
        getNodes(param).let { nodes ->
            graph.beginEdit(id, nodes + nodes.map { it.tag }).use {
                RotateTransformation.rotate(graph, nodes, RotateTransformation.RotationDirection.COUNTER_CLOCKWISE, graphComponent.createRenderContext())
            }
        }
        graph.invalidateDisplays()
    }
}

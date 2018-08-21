/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.MutableRectangle
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.INode
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.type

object SbgnGroupingSupport {

    fun calculatePreferredGroupBounds(graph: IGraph, group: INode): RectD {
        return when(group.type) {
            SbgnType.COMPARTMENT -> MutableRectangle().apply { graph.getChildren(group).forEach { add(it.layout.center) }}.toRectD()
            else -> MutableRectangle().apply { graph.getChildren(group).forEach { add(it.layout) }}.toRectD().getEnlarged(10.0)
        }
    }
}
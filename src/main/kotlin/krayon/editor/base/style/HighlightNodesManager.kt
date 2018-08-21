/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.GraphComponent
import com.yworks.yfiles.view.IVisualTemplate
import com.yworks.yfiles.view.Pen
import com.yworks.yfiles.view.RectangleIndicatorInstaller
import java.awt.Color

open class HighlightNodesManager(val graphComponent:GraphComponent) {

    private val highlightedNodeSet = mutableSetOf<INode>()

    fun isHighlighted(node: INode):Boolean {
        return highlightedNodeSet.contains(node)
    }

    protected open fun createVisualTemplate(node:INode, tag:String):IVisualTemplate {
        return LabeledHighlightNodeVisualTemplate(Pen.getBlack(), tag, Color.WHITE)
    }

    fun addHighlight(node: INode, text:String) {
        with(graphComponent) {
            val prefTemplate = getClientProperty(RectangleIndicatorInstaller.HIGHLIGHT_TEMPLATE_KEY)
            putClientProperty(RectangleIndicatorInstaller.HIGHLIGHT_TEMPLATE_KEY, createVisualTemplate(node, text))
            highlightIndicatorManager.addHighlight(node)
            putClientProperty(RectangleIndicatorInstaller.HIGHLIGHT_TEMPLATE_KEY, prefTemplate)
            highlightedNodeSet.add(node)
        }
    }

    fun clearHighlights() {
        highlightedNodeSet.forEach { graphComponent.highlightIndicatorManager.removeHighlight(it) }
    }

    fun removeHighlight(node: INode) {
        highlightedNodeSet.remove(node)
        graphComponent.highlightIndicatorManager.removeHighlight(node)
    }
}
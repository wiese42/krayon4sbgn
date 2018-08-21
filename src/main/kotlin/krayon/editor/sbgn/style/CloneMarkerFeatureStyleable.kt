/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.graph.INode
import krayon.editor.base.model.IModelItemFeature
import krayon.editor.base.style.DefaultStyleableContext
import krayon.editor.base.style.IStyleable
import krayon.editor.base.style.IStyleableContext
import krayon.editor.base.style.StyleProperty
import java.awt.Paint

class CloneMarkerFeatureStyleable : IStyleable {
    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        val node = (context.item as? IModelItemFeature)?.owner as? INode
        val nodeStyle = node?.style as? SbgnMultimerStyle
        if(nodeStyle != null) {
            (map[StyleProperty.CloneMarkerBackground] as? Paint)?.let { nodeStyle.clonePaint = it }
            SbgnBuilder.applyStyleToCloneMarkerLabel(DefaultStyleableContext(node, context.graph, context.graphComponent), map)
        }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        val node = (context.item as? IModelItemFeature)?.owner as? INode
        val nodeStyle = node?.style as? SbgnMultimerStyle
        if(nodeStyle != null) {
            map[StyleProperty.CloneMarkerBackground] = nodeStyle.clonePaint
            SbgnBuilder.retrieveStyleFromCloneMarkerLabel(DefaultStyleableContext(node, context.graph, context.graphComponent), map)
        }
    }
}
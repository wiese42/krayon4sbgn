/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.graph.*
import krayon.editor.base.io.StyleIO
import krayon.editor.base.model.IModelItemFeatureProvider
import krayon.editor.base.util.withHeight
import krayon.editor.base.util.withWidth
import java.io.*

class StyleManager<T>(private val typeMapper:(IModelItem) -> T, private val styleSetter:(IModelItem, GraphStyle<T>) -> Unit) {

    var styleIO:StyleIO<T> = StyleIO()

    private val _styles = mutableListOf<GraphStyle<T>>()
    var currentStyle: GraphStyle<T>? = null
        set(value) {
            field = value
            styleListeners.forEach { it.onStyleEvent(value!!, StyleOp.CURRENT_STYLE_CHANGED ) }
        }

    val styles:List<GraphStyle<T>> get() = _styles
    val styleListeners = mutableListOf<StyleListener<T>>()

    fun addStyle(style: GraphStyle<T>) {
        _styles.add(style)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun removeStyle(style: GraphStyle<T>) {
        _styles.remove(style)
    }

    fun applyStyle(graphStyle: GraphStyle<T>, graph: IGraph, applySize:Boolean = true) {
        for (node in graph.nodes) {
            applyStyle(graphStyle, graph, node, applySize)
            for (label in node.labels) {
                applyStyle(graphStyle, graph, label, applySize)
            }
            for (port in node.ports) {
                applyStyle(graphStyle, graph, port, applySize)
            }
        }
        for (edge in graph.edges) {
            applyStyle(graphStyle, graph, edge, applySize)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun applyStyle(graphStyle: GraphStyle<T>, graph: IGraph, item: IModelItem, applySize: Boolean = true) {
        //val graphStyle = item.graphStyle ?: (item as? ILabel)?.owner?.graphStyle ?: defaultStyle
        val styleMap = graphStyle.styleTemplateMap[typeMapper(item)] ?: return
        val node = item as? INode
        if(node != null) {

            styleSetter.invoke(node, graphStyle)

            if(applySize) {
                (styleMap[StyleProperty.Width] as? Double?)?.let { styleWidth ->
                    graph.setNodeLayout(node, node.layout.withWidth(styleWidth))
                }
                (styleMap[StyleProperty.Height] as? Double?)?.let { styleHeight ->
                    graph.setNodeLayout(node, node.layout.withHeight(styleHeight))
                }
            }
            (node.style as? IStyleable)?.applyStyle(node, graph, styleMap)

            node.lookup(IModelItemFeatureProvider::class.java)?.getFeatures(node)?.forEach { nodeFeature ->
                graphStyle.styleTemplateMap[typeMapper(nodeFeature)]?.let { featureStyleMap ->
                    nodeFeature.style?.applyStyle(nodeFeature, graph, featureStyleMap)
                }
            }
            applyStylesToNodeLabelsAndPorts(graphStyle.styleTemplateMap, graph, node, applySize)
        }
        val label = item as? ILabel
        (label?.style as? IStyleable)?.applyStyle(label, graph, styleMap)
        val edge = item as? IEdge
        if(edge != null) {
            styleSetter.invoke(edge, graphStyle)
            (edge.style as? IStyleable)?.applyStyle(edge, graph, styleMap)
            applyStylesToEdgeLabels(graphStyle.styleTemplateMap, graph, edge)
        }
        val port = item as? IPort
        if(port != null) applyStyleToPort(port, graph, styleMap, applySize)
    }

    private fun applyStyleToPort(port:IPort, graph:IGraph, styleMap: StyleAttributes, applySize: Boolean) {
        if(!applySize) {
            val width = styleMap.remove(StyleProperty.Width)
            val height = styleMap.remove(StyleProperty.Height)
            (port.style as? IStyleable)?.applyStyle(port, graph, styleMap)
            width?.let { styleMap[StyleProperty.Width] = it }
            height?.let { styleMap[StyleProperty.Height] = it }
        }
        else (port.style as? IStyleable)?.applyStyle(port, graph, styleMap)
    }

    private fun applyStylesToNodeLabelsAndPorts(templateMap: StyleTemplateMap<T>, graph: IGraph, node:INode, applySize: Boolean) {
        //node.labels.filter { it.type.isAuxUnit() }.forEach { label ->
        node.labels.filter { typeMapper(it) != null }.forEach { label ->
            val styleMap = templateMap[typeMapper(label)]
            if (styleMap != null) {
                (label.style as IStyleable).applyStyle(label, graph, styleMap)
            }
        }
        //node.ports.filter { it.type == SbgnType.TERMINAL }.forEach { port ->
        node.ports.filter { typeMapper(it) != null }.forEach { port ->
            val styleMap = templateMap[typeMapper(port)]
            if (styleMap != null) applyStyleToPort(port, graph, styleMap, applySize)
        }
    }

    private fun applyStylesToEdgeLabels(templateMap: StyleTemplateMap<T>, graph: IGraph, edge:IEdge) {
        edge.labels.filter { typeMapper(it) != null }.forEach { label ->
            val styleMap = templateMap[typeMapper(label)]
            if (styleMap != null) {
                (label.style as IStyleable).applyStyle(label, graph, styleMap)
            }
        }
    }

    fun createStyle(name:String, templateStyle: GraphStyle<T>):GraphStyle<T> {
        val newTemplateMap = templateStyle.styleTemplateMap.toMutableMap()
        newTemplateMap.keys.forEach { key ->
            val newStyleMap = newTemplateMap[key]!!.toMutableMap()
            newTemplateMap[key] = newStyleMap
        }
        val newStyle = GraphStyle(name, false, newTemplateMap)
        styleListeners.forEach { it.onStyleEvent(newStyle, StyleOp.CREATE) }
        return newStyle
    }

    /**
     *  Sync with default style properties, i.e. remove all properties that are missing in default style,
     *  and add all missing properties that are defined in default style.
     */
    fun normalizeStyle(graphStyle: GraphStyle<T>) {
        val defaultStyle = getDefaultStyle()

        //remove all properties that are missing in default style
        graphStyle.styleTemplateMap.keys.toList().forEach { typeKey ->
            if(!defaultStyle.styleTemplateMap.containsKey(typeKey)) {
                graphStyle.styleTemplateMap.remove(typeKey)
            }
            else {
                val defaultStyleMap = defaultStyle.styleTemplateMap[typeKey]!!
                val styleMap = graphStyle.styleTemplateMap[typeKey]!!
                styleMap.keys.toList().forEach { key ->
                    if(!defaultStyleMap.containsKey(key)) {
                        styleMap.remove(key)
                    }
                }
            }
        }

        //add all missing properties that are defined in default style
        defaultStyle.styleTemplateMap.keys.forEach { typeKey ->
            if(!graphStyle.styleTemplateMap.containsKey(typeKey)) {
                graphStyle.styleTemplateMap[typeKey] = defaultStyle.styleTemplateMap[typeKey]!!.toMutableMap()
            }
            else {
                val defaultStyleMap = defaultStyle.styleTemplateMap[typeKey]!!
                val styleMap = graphStyle.styleTemplateMap[typeKey]!!
                defaultStyleMap.keys.forEach { key ->
                    if(!styleMap.containsKey(key)) {
                        styleMap[key] = defaultStyleMap[key]
                    }
                }
            }
        }
    }

    fun updateStyle(graphStyle: GraphStyle<T>, types:List<T>, styleKey: StyleProperty, value:Any?) {
        for (type in types) {
            val targetStyleMap = graphStyle.styleTemplateMap[type] ?: mutableMapOf()
            targetStyleMap[styleKey] = value
            graphStyle.styleTemplateMap[type] = targetStyleMap
        }
        styleListeners.forEach { it.onStyleEvent(graphStyle, StyleOp.MODIFY) }
    }

    fun deleteStyle(graphStyle: GraphStyle<T>) {
        removeStyle(graphStyle)
        styleListeners.forEach { it.onStyleEvent(graphStyle, StyleOp.DELETE) }
    }

    fun writeStyleToDir(graphStyle:GraphStyle<T>, styleDir:String) {
        val file = File(styleDir, graphStyle.name + ".css")
        file.parentFile.mkdirs()
        FileWriter(file).use {
            styleIO.writeStyleMap(it, graphStyle)
        }
    }

    fun addStyleFromStream(stream: InputStream, name:String, readOnly:Boolean) {
        InputStreamReader(stream).use {
            val templateMap = styleIO.readCss(it)
            addDefaults(templateMap)
            val graphStyle = GraphStyle(name, readOnly, templateMap)
            normalizeStyle(graphStyle)
            addStyle(graphStyle)
        }
    }

    private fun addDefaults(styleMap:StyleTemplateMap<T>)  {
        getDefaultStyle().styleTemplateMap.entries.forEach { (type, templateMap) ->
            templateMap.entries.forEach { (key, value) ->
                if(styleMap[type]?.containsKey(key) == false) styleMap[type]?.set(key,value)
            }
        }
    }

    private fun addStyleFromFile(file:File, readOnly:Boolean) {
        FileReader(file).use {
            val templateMap = styleIO.readCss(it)
            val graphStyle = GraphStyle(file.nameWithoutExtension, readOnly, templateMap)
            addDefaults(templateMap)
            addStyle(graphStyle)
        }
    }

    fun addStylesFromDir(dirPath:String, readOnly:Boolean) {
        val dir = File(dirPath)
        if(dir.exists()) {
            File(dirPath).list { _, name -> name.endsWith(".css") }.forEach { name ->
                addStyleFromFile(File(dirPath, name), readOnly)
            }
        }
    }

    fun deleteStyleFromDir(graphStyle: GraphStyle<T>, dirPath:String) {
        File(dirPath, graphStyle.name + ".css").deleteOnExit()
    }

    /**
     * First isFileLocal, then read-only, then !read-only.
     */
    fun getStylesInDisplayOrder():Iterable<GraphStyle<T>> {
        val result = mutableListOf<GraphStyle<T>>()
        for (style in styles)
            if (style.isFileLocal) result += style
        for (style in styles)
            if (style.isReadOnly && !style.isFileLocal) result += style
        for (style in styles)
            if (!style.isReadOnly && !style.isFileLocal) result += style
        return result
    }

    fun getDefaultStyle(): GraphStyle<T> {
        return styles[0]
    }

    fun fireStyleEvent(graphStyle:GraphStyle<T>, op:StyleOp) {
        styleListeners.forEach{ it.onStyleEvent(graphStyle, op)}
    }

    enum class StyleOp { DELETE, CREATE, MODIFY, CURRENT_STYLE_CHANGED, SHOW_EDITOR, HIDE_EDITOR }
    interface StyleListener<T> {
        fun onStyleEvent(graphStyle:GraphStyle<T>, op:StyleOp)
    }
}
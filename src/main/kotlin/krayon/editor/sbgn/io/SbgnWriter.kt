/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.io

import com.sun.xml.internal.bind.marshaller.CharacterEscapeHandler
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.IModelItem
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.IPort
import krayon.editor.base.style.GraphStyle
import krayon.editor.base.util.convertFromRatioPoint
import krayon.editor.base.util.plus
import krayon.editor.sbgn.model.*
import krayon.editor.sbgn.ui.SbgnGraphComponent
import org.sbgn.GlyphClazz
import org.sbgn.bindings.*
import org.sbgn.bindings.Map
import org.w3c.dom.Node
import java.io.OutputStream
import java.io.StringWriter
import java.io.Writer
import javax.xml.bind.JAXBContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class SbgnWriter(val includeStyle:Boolean = true) {

    private val domCreator = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    private val krayonNS = "http://krayon.sbgn.ns/v1"
    private val styleIdMap = mutableMapOf<GraphStyle<SbgnType>,String>()

    private fun RectD.toBbox():Bbox {
        val bbox = Bbox()
        bbox.x = x.toFloat()
        bbox.y = y.toFloat()
        bbox.w = width.toFloat()
        bbox.h = height.toFloat()
        return bbox
    }

    private fun PointD.toPoint():Point {
        val point = Point()
        point.x = x.toFloat()
        point.y = y.toFloat()
        return point
    }

    private var curId:Int = 0
    private val portMap = HashMap<IPort, Any>()

    private fun nextId() = "id${++curId}"

    private fun createMapExtensionNode(graph: IGraph, graphComponent:SbgnGraphComponent?):Node? {
        val graphStyles = (graph.nodes+graph.edges).mapNotNull { it.graphStyle }.toMutableSet()
        graphComponent?.graphStyle?.let { graphStyles += it }

        return if(graphStyles.any()) {
            domCreator.createElement("extension").apply {
                val styles = domCreator.createElementNS(krayonNS, "styles").apply {
                    setAttribute("version", "1")
                    graphStyles.forEach { style ->
                        appendChild(domCreator.createElementNS(krayonNS, "style").apply {
                            setAttribute("id", getStyleId(style))
                            setAttribute("name", style.name)
                            val writer = StringWriter()
                            val styleIO = SbgnStyleIO()
                            styleIO.writeStyleMap(writer, style)
                            //work-around: avoid unnecessary blank lines on Windows
                            val css = writer.toString().replace("\r", "")
                            //println(css)
                            appendChild(domCreator.createCDATASection("\n$css"))
                        })
                    }
                }
                appendChild(styles)
                graphComponent?.graphStyle?.let {
                    val style = domCreator.createElementNS(krayonNS, "style").apply {
                        setAttribute("idRef", getStyleId(it))
                    }
                    appendChild(style)
                }
            }

        }
        else null
    }

    fun write(output: OutputStream, graph: IGraph, graphComponent: SbgnGraphComponent?) {
        curId = 0
        portMap.clear()

        val sbgn = Sbgn().apply {

        }
        val map = Map().apply {
            language = "process description"
        }

        styleIdMap.clear()

        sbgn.map = map
        val nodes = graph.nodes.filter { graph.getParent(it) == null }
        writeNodes(graph, map.glyph, nodes)
        writeEdges(graph, map)

        val context = JAXBContext.newInstance("org.sbgn.bindings")
        val marshaller = context.createMarshaller()

        marshaller.setProperty(CharacterEscapeHandler::class.java.name, CustomCharacterEscapeHandler())
        marshaller.setProperty("jaxb.formatted.output", java.lang.Boolean.TRUE)

        marshaller.marshal(sbgn, domCreator)
        val mapNode = domCreator.childNodes.item(0).childNodes.item(0)

        if(includeStyle) {
            createMapExtensionNode(graph, graphComponent)?.also { mapNode.insertBefore(it, mapNode.firstChild) }
        }

        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty(OutputKeys.METHOD, "xml")
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        }
        transformer.transform(DOMSource(domCreator), StreamResult(output))
    }

    private fun getStyleId(graphStyle:GraphStyle<SbgnType>):String {
        var id = styleIdMap[graphStyle]
        if(id == null) {
            id = "style${styleIdMap.size+1}"
            styleIdMap[graphStyle] = id
        }
        return id
    }

    private fun createMapExtension(item:IModelItem):SBGNBase.Extension? {
        return if (item.graphStyle != null) {
            SBGNBase.Extension().apply {
                any.add(domCreator.createElementNS(krayonNS, "style").apply {
                    setAttribute("idRef", getStyleId(item.graphStyle!!))
                })
            }
        } else null
    }

    private fun writeNodes(graph:IGraph, parentGlyph:MutableList<Glyph>, nodes:List<INode>, compartmentRef:Glyph? = null) {
        nodes.forEach { node ->
            parentGlyph.add(Glyph().apply {
                val glyph = this
                id = nextId()
                if(includeStyle) extension = createMapExtension(node)

                clazz = IOTypeMapper.getGlyphClazz(node)

                bbox = if(node.type.isPN() || node.type.isLogic()) {
                    val dim = Math.min(node.layout.width, node.layout.height)
                    val x = node.layout.x + 0.5*(node.layout.width - dim)
                    val y = node.layout.y + 0.5*(node.layout.height - dim)
                    RectD(x,y,dim, dim).toBbox()
                }
                else node.layout.toRectD().toBbox()

                if(compartmentRef != null) this.compartmentRef = compartmentRef

                node.labels.firstOrNull { it.type == SbgnType.NAME_LABEL }?.let { nameLabel ->
                    label = Label().apply {
                        text = nameLabel.text
                        bbox = nameLabel.layout.bounds.toBbox()
                    }
                }

                node.labels.filter { it.type == SbgnType.STATE_VARIABLE || it.type == SbgnType.UNIT_OF_INFORMATION }.forEach {
                    glyph.glyph.add(Glyph().apply {
                        id = nextId()
                        clazz = IOTypeMapper.getGlyphClazz(it)
                        bbox = it.layout.bounds.toBbox()
                        if(it.type == SbgnType.STATE_VARIABLE) {
                            val splitIndex = it.text.indexOf('@')
                            state = Glyph.State()
                            if(splitIndex >= 0) {
                                state.value = it.text.substring(0,splitIndex)
                                state.variable = it.text.substring(splitIndex+1)
                            }
                            else {
                                state.value = it.text
                            }
                        }
                        else {
                            label = Label()
                            label.text = it.text
                        }
                    })
                }

                node.labels.filter { it.type == SbgnType.CALLOUT_LABEL }.forEach {
                    parentGlyph.add(Glyph().apply {
                        id = nextId()
                        clazz = GlyphClazz.ANNOTATION.clazz
                        bbox = it.layout.bounds.toBbox()
                        callout = Glyph.Callout().apply {
                            target = glyph
                            val anchor = it.getSbgnProperty (SbgnPropertyKey.CALLOUT_POINT) as? PointD ?: PointD.ORIGIN
                            point = node.layout.convertFromRatioPoint(anchor).toPoint()
                        }
                        label = Label().apply {
                            text = it.text
                        }
                    })
                }

                node.ports.forEach {
                    if(node.type == SbgnType.SUBMAP && it.type == SbgnType.TERMINAL) {
                        val terminalGlyph = Glyph().apply {
                            id = nextId()
                            clazz = IOTypeMapper.getGlyphClazz(it)
                            orientation = it.getSbgnProperty(SbgnPropertyKey.ORIENTATION) as String
                            val size = it.getSbgnProperty(SbgnPropertyKey.TERMINAL_SIZE) as SizeD
                            val offset = when(orientation) {
                                "left" -> PointD(-size.width * 0.5, 0.0)
                                "right" -> PointD(size.width * 0.5, 0.0)
                                "up" -> PointD(0.0, -size.height * 0.5)
                                "down" -> PointD(0.0, size.height * 0.5)
                                else -> PointD.ORIGIN
                            }
                            bbox = RectD.fromCenter(it.location+offset, size).toBbox()
                            //println("y: write portbox = " + (bbox.y + bbox.h*0.5)  + " port.loc=" + it.location.y)
                            //println("x: write portbox = " + (bbox.x+ bbox.w*0.5)  + " port.loc=" + it.location.x)

                            label = Label()
                            label.text = it.getSbgnProperty(SbgnPropertyKey.TERMINAL_LABEL) as? String
                        }
                        portMap[it] = terminalGlyph
                        glyph.glyph.add(terminalGlyph)
                    }
                    else {
                        val newPort = Port().apply {
                            id = nextId()
                            x = it.location.x.toFloat()
                            y = it.location.y.toFloat()
                        }
                        portMap[it] = newPort
                        port.add(newPort)
                    }
                }

                if(node.isClone) {
                    glyph.clone = Glyph.Clone()
                    node.labels.firstOrNull { it.type == SbgnType.CLONE_LABEL }?.let {
                        glyph.clone.label = Label().apply {
                            text = it.text
                            bbox = it.layout.bounds.toBbox()
                        }
                    }
                }

                when(node.type) {
                    SbgnType.COMPARTMENT -> {
                        compartmentOrder = curId.toFloat()
                        writeNodes(graph, parentGlyph, graph.getChildren(node).toList(), glyph)
                    }
                    SbgnType.COMPLEX -> {
                        writeNodes(graph, glyph.glyph, graph.getChildren(node).toList())
                    }
                    SbgnType.TAG -> {
                        glyph.orientation = node.orientation
                    }
                    else -> { }
                }
            })
        }
    }

    private fun writeEdges(graph:IGraph, map:Map) {
        graph.edges.forEach { edge ->
            map.arc.add(Arc().apply {
                id = nextId()
                if(includeStyle) extension = createMapExtension(edge)
                clazz = IOTypeMapper.getGlyphClazz(edge)
                val pathGeom = edge.style.renderer.getPathGeometry(edge, edge.style)
                val cursor = pathGeom.path.createCursor()
                cursor.moveNext()
                val startPoint = cursor.currentEndPoint
                start = Arc.Start().apply {
                    x = startPoint.x.toFloat()
                    y = startPoint.y.toFloat()
                }
                cursor.toLast()
                val endPoint = cursor.currentEndPoint
                end = Arc.End().apply {
                    x = endPoint.x.toFloat()
                    y = endPoint.y.toFloat()
                }
                source = portMap[edge.sourcePort]
                target = portMap[edge.targetPort]

                edge.bends.forEach { bend ->
                    next.add(Arc.Next().apply {
                        x = bend.location.x.toFloat()
                        y = bend.location.y.toFloat()
                    })
                }

                val cardinalityLabel = edge.labels.firstOrNull { it.type == SbgnType.CARDINALITY }
                if(cardinalityLabel != null) {
                    glyph.add(Glyph().apply {
                        id = nextId()
                        clazz = IOTypeMapper.getGlyphClazz(cardinalityLabel)
                        label = Label()
                        label.text = cardinalityLabel.text
                        bbox = cardinalityLabel.layout.bounds.toBbox()
                    })
                }
            })
        }
    }

    class CustomCharacterEscapeHandler : CharacterEscapeHandler {
        override fun escape(ch: CharArray, _start: Int, length: Int, isAttVal: Boolean, out: Writer) {
            var start = _start
            // avoid calling the Writerwrite method too much by assuming
            // that the escaping occurs rarely.
            // profiling revealed that this is faster than the naive code.
            val limit = start + length
            for (i in start until limit) {
                val c = ch[i]
                if (c == '\n' || c == '&' || c == '<' || c == '>' || c == '\'' || c == '\"' && isAttVal) {
                    if (i != start)
                        out.write(ch, start, i - start)
                    start = i + 1
                    when (ch[i]) {
                        '&' -> out.write("&amp;")
                        '<' -> out.write("&lt;")
                        '>' -> out.write("&gt;")
                        '\"' -> out.write("&quot;")
                        '\'' -> out.write("&apos;")
                        '\n' -> out.write("&#xA;")
                    }
                }
            }

            if (start != limit)
                out.write(ch, start, limit - start)
        }
    }

}


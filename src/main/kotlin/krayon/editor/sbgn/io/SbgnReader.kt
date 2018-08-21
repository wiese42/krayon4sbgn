/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.io

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.portlocationmodels.FreeNodePortLocationModel
import krayon.editor.base.style.GraphStyle
import krayon.editor.base.style.StyleProperty
import krayon.editor.base.style.applyStyle
import krayon.editor.base.util.convertToRatioPoint
import krayon.editor.base.util.getMainCanvasObject
import krayon.editor.base.util.minus
import krayon.editor.sbgn.model.*
import krayon.editor.sbgn.style.SbgnBuilder
import krayon.editor.sbgn.ui.SbgnGraphComponent
import krayon.util.asList
import org.sbgn.ArcClazz
import org.sbgn.GlyphClazz
import org.sbgn.bindings.*
import org.sbgn.bindings.Map
import java.io.InputStream
import java.io.StringReader
import javax.xml.bind.JAXBContext

class SbgnReader {

    private val krayonNS = "http://krayon.sbgn.ns/v1"
    private val idStyleMap = mutableMapOf<String,GraphStyle<SbgnType>>()

    private fun Bbox.toRectD() = RectD(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble())
    private fun Point.toPointD() = PointD(x.toDouble(), y.toDouble())
    private fun Port.toPointD() = PointD(x.toDouble(), y.toDouble())
    private fun Arc.End.toPointD() = PointD(x.toDouble(), y.toDouble())
    private fun Arc.Start.toPointD() = PointD(x.toDouble(), y.toDouble())
    private fun Arc.Next.toPointD() = PointD(x.toDouble(), y.toDouble())
    private fun Glyph.isSupportingLabel(): Boolean {
        return clazz != GlyphClazz.OR.toString() && clazz != GlyphClazz.AND.toString() && clazz != GlyphClazz.NOT.toString()
    }
    private fun Glyph.ofClazz(vararg clazz: GlyphClazz): Boolean {
        return clazz.any { this.clazz == it.toString() }
    }
    private fun Arc.ofClazz(vararg clazz: ArcClazz): Boolean {
        return clazz.any { this.clazz == it.toString() }
    }

    fun read(input: InputStream, graph: IGraph, graphComponent:SbgnGraphComponent?) {

        val context = JAXBContext.newInstance("org.sbgn.bindings")
        val unmarshaller = context.createUnmarshaller()

        //skip this. extensions crash the parser if namespace tweaker is installed. why?
        //val reader = XMLReaderFactory.createXMLReader()
        //val inFilter = NamespaceFilter("http://sbgn.org/libsbgn/0.2")
        //inFilter.parent = reader
        //val source = SAXSource(inFilter, InputSource(input))
        //val sbgn = unmarshaller.unmarshal(source) as Sbgn

        val sbgn = unmarshaller.unmarshal(input) as Sbgn

        // map is a container for the glyphs and arcs
        val map = sbgn.map

        if (map.language != "process description") {
            throw UnsupportedOperationException("Support for diagram type ${map.language} not implemented yet.")
        }

        processStyleDefinitions(map.extension)
        if(graphComponent != null) {
            val mapStyle = getGraphStyle(map.extension) ?: SbgnBuilder.styleManager.currentStyle
            if(mapStyle != null) {
                graphComponent.graphStyle = mapStyle
                mapStyle.styleTemplateMap[SbgnType.MAP]?.let {
                    graphComponent.applyStyle(graphComponent, it)
                }
            }
        }

        // we can get a list of glyphs (nodes) in this map with getGlyph()

        val idMap = HashMap<String, IModelItem>()
        processGlyphs(graph, idMap, map.glyph, null)

        processArcs(graph, idMap, map)

        configurePorts(graph)
        if(graphComponent != null) establishDrawingOrder(graphComponent)
    }

    private fun processStyleDefinitions(ext:SBGNBase.Extension?) {
        idStyleMap.clear()
        ext?.any?.forEach { styles ->
            if(styles.nodeName == "styles" && styles.namespaceURI == krayonNS) {
                styles.getElementsByTagName("style").asList().forEach { style ->
                    val id = style.attributes.getNamedItem("id")?.nodeValue!!
                    val name = style.attributes.getNamedItem("name")?.nodeValue ?: id
                    val reader = StringReader(style.textContent)
                    val templateMap = SbgnStyleIO().readCss(reader)
                    val graphStyle = GraphStyle(name, false, templateMap).apply { isFileLocal = true }
                    SbgnBuilder.styleManager.normalizeStyle(graphStyle)
                    idStyleMap[id] = graphStyle
                }
            }
        }
    }

    private fun establishDrawingOrder(graphComponent: SbgnGraphComponent) {
        graphComponent.graph.nodes.filter { it.type == SbgnType.COMPARTMENT }.reversed().forEach {
            graphComponent.graphModelManager.getMainCanvasObject(it).toBack()
        }
    }

    private fun configurePorts(graph:IGraph) {
        //(I) set port type according to connected edges
        for (edge in graph.edges) {
            if (edge.sourceNode.type.isPN() && (edge.type == SbgnType.CONSUMPTION || edge.type == SbgnType.PRODUCTION))
                edge.sourcePort.type = SbgnType.INPUT_AND_OUTPUT
            else if(edge.sourceNode.type.isLogic() && (edge.type == SbgnType.LOGIC_ARC || edge.type.isRegulation()))
                edge.sourcePort.type = SbgnType.INPUT_AND_OUTPUT

            if (edge.targetNode.type.isPN() && (edge.type == SbgnType.CONSUMPTION || edge.type == SbgnType.PRODUCTION))
                edge.targetPort.type = SbgnType.INPUT_AND_OUTPUT
            else if(edge.targetNode.type.isLogic() && (edge.type == SbgnType.LOGIC_ARC || edge.type.isRegulation()))
                edge.targetPort.type = SbgnType.INPUT_AND_OUTPUT

        }
        //(II) set port type according to orientation and approximate port location
        for (node in graph.nodes) {
            if (node.type.isPN()) {
                val box = node.layout.toRectD()
                val isHorizontal = node.orientation == "horizontal"
                node.ports.forEach { port ->
                    if (graph.edgesAt(port).size() == 0) {
                        val p = box.convertToRatioPoint(port.location)
                        if ((p.x < 0.25 || p.x > 0.75) && isHorizontal) {
                            port.type = SbgnType.INPUT_AND_OUTPUT
                        } else if ((p.y < 0.25 || p.y > 0.75) && !isHorizontal) {
                            port.type = SbgnType.INPUT_AND_OUTPUT
                        }
                    }
                }
            }
        }
        //add missing ports to PNs and Logic. Heuristic adds an extra port to unoccupied side.
        for (node in graph.nodes) {
            if (node.type.isPN() || node.type.isLogic()) {
                val isHorizontal = node.orientation == "horizontal"
                if(isHorizontal) {
                    if (node.ports.none { node.layout.convertToRatioPoint(it.location).x < 0.25 }) {
                        graph.addPort(node, FreeNodePortLocationModel.NODE_LEFT_ANCHORED).let {
                            if (isHorizontal) it.type = SbgnType.INPUT_AND_OUTPUT
                        }
                    }
                    if (node.ports.none { node.layout.convertToRatioPoint(it.location).x > 0.75 }) {
                        graph.addPort(node, FreeNodePortLocationModel.NODE_RIGHT_ANCHORED).let {
                            if (isHorizontal) it.type = SbgnType.INPUT_AND_OUTPUT
                        }
                    }
                }
                else if(!isHorizontal) {
                    if (node.ports.none { node.layout.convertToRatioPoint(it.location).y < 0.25 }) {
                        graph.addPort(node, FreeNodePortLocationModel.NODE_TOP_ANCHORED).let {
                            if (!isHorizontal) it.type = SbgnType.INPUT_AND_OUTPUT
                        }
                    }
                    if (node.ports.none { node.layout.convertToRatioPoint(it.location).y > 0.75 }) {
                        graph.addPort(node, FreeNodePortLocationModel.NODE_BOTTOM_ANCHORED).let {
                            if (!isHorizontal) it.type = SbgnType.INPUT_AND_OUTPUT
                        }
                    }
                }
            }
        }
    }

    private fun processArcs(graph:IGraph, idMap: HashMap<String, IModelItem>, map: Map) {
        for (arc in map.arc) {

            var sourcePort: IPort? = null
            var sourceNode: INode? = null
            if (arc.source is Port) {
                sourcePort = idMap[(arc.source as Port).id] as IPort
            } else if (arc.source is Glyph) {
                val glyphId = (arc.source as Glyph).id
                when {
                    idMap[glyphId] is INode -> sourceNode = idMap[glyphId] as INode
                    idMap[glyphId] is IPort -> sourcePort = idMap[glyphId] as IPort
                    else -> println("source of unknown type")
                }
            }

            var targetPort: IPort? = null
            var targetNode: INode? = null
            if (arc.target is Port) {
                targetPort = idMap[(arc.target as Port).id] as IPort
            } else if (arc.target is Glyph) {
                val glyphId = (arc.target as Glyph).id
                when {
                    idMap[glyphId] is INode -> targetNode = idMap[glyphId] as INode
                    idMap[glyphId] is IPort -> targetPort = idMap[glyphId] as IPort
                    else -> println("target of unknown type")
                }
            }

            if (sourcePort == null) {
                sourcePort = graph.addPort(sourceNode)
                if (arc.start != null) {
                    graph.setPortLocation(sourcePort, getPortLocation(arc, sourceNode!!,atSource = true ))
                }
            }

            if (targetPort == null) {
                targetPort = graph.addPort(targetNode)
                if (arc.end != null) {
                    graph.setPortLocation(targetPort, getPortLocation(arc, targetNode!!, atSource = false))
                }
            }

            var edge: IEdge?
            if (sourcePort != null && targetPort != null) {
                edge = graph.createEdge(sourcePort, targetPort)
            } else {
                println("can't find source/target for edge")
                continue
            }

            if (IOTypeMapper.getSbgnType(arc) == SbgnType.NO_TYPE) {
                println("can't handle arc type " + arc.clazz)
            } else {
                edge.type = IOTypeMapper.getSbgnType(arc)
            }

            arc.next.forEach {
                graph.addBend(edge, it.toPointD())
            }

            if (arc.ofClazz(ArcClazz.PRODUCTION, ArcClazz.CONSUMPTION)) {
                val cardinalityGlyph = arc?.glyph?.firstOrNull { it.ofClazz(GlyphClazz.CARDINALITY) }
                if (cardinalityGlyph != null) {
                    if (cardinalityGlyph.label?.text != null) {
                        val label = graph.addLabel(edge, cardinalityGlyph.label.text)
                        label.type = SbgnType.CARDINALITY
                        SbgnBuilder.configure(graph, label, cardinalityGlyph.bbox?.toRectD())
                    }
                }
            }

            edge.graphStyle = getGraphStyle(arc.extension)

            SbgnBuilder.configure(graph, edge)
        }
    }

    private fun getEdgePoints(arc:Arc, atSource:Boolean):Pair<PointD, PointD> {
        return if(atSource) {
            val p1 = if (arc.next.any()) arc.next.first().toPointD() else arc.end.toPointD()
            val p2 = arc.start.toPointD()
            Pair(p1,p2)
        }
        else {
            val p1 = if(arc.next.any()) arc.next.last().toPointD() else arc.start.toPointD()
            val p2 = arc.end.toPointD()
            Pair(p1,p2)
        }
    }

    private fun getPortLocation(arc: Arc, terminal:INode, atSource:Boolean):PointD {
        return if(terminal.type.isLogic() || terminal.type.isPN() && arc.ofClazz(ArcClazz.CONSUMPTION, ArcClazz.PRODUCTION)) {
            arc.start.toPointD()
        } else {
            val (p1,p2) = getEdgePoints(arc, atSource)
            val projection = terminal.layout.center.getProjectionOnLine(p1, p2-p1)
            val distance = projection.distanceTo(terminal.layout.center)
            if(distance < 5.0) terminal.layout.center else p2
        }
    }

    private fun processGlyphs(graph: IGraph, idMap: HashMap<String, IModelItem>, glyphs: List<Glyph>, parent: Glyph?) {
        //println("processGlyphs for parent " + ((parent?.clazz) ?: "MAP") + " " + (parent?.label?.text ?: "unnamed"))
        val isCompartment = { it: Glyph -> it.clazz == GlyphClazz.COMPARTMENT.toString() }
        val isLabel = { it: Glyph -> it.ofClazz(GlyphClazz.UNIT_OF_INFORMATION, GlyphClazz.STATE_VARIABLE) ||
                it.ofClazz(GlyphClazz.ANNOTATION) && it.callout?.target != null
        }
        val isPort = { it: Glyph -> it.ofClazz(GlyphClazz.TERMINAL) }

        val compartmentGlyphs = glyphs.filter(isCompartment).sortedBy { it -> it.compartmentOrder }
        val labelGlyphs = glyphs.filter(isLabel)
        val nodeGlyphs = glyphs.filter { !isPort(it) && !isLabel(it) && !isCompartment(it) }
        val portGlyphs = glyphs.filter(isPort)

        for (glyph in compartmentGlyphs + nodeGlyphs) {
            //println(" Glyph with class " + glyph.clazz + " label=" + glyph.label?.text)
            if (IOTypeMapper.getSbgnType(glyph) == SbgnType.NO_TYPE) {
                println("Can't handle glyph of type " + glyph.clazz)
                continue
            }

            var rect = if (glyph.bbox != null) glyph.bbox.toRectD() else null

            if(rect != null) {
                if (glyph.ofClazz(GlyphClazz.OR, GlyphClazz.AND, GlyphClazz.NOT, GlyphClazz.PROCESS,
                                GlyphClazz.ASSOCIATION, GlyphClazz.DISSOCIATION, GlyphClazz.UNCERTAIN_PROCESS,
                                GlyphClazz.OMITTED_PROCESS)) {
                    glyph.port.forEach { port ->
                        rect = RectD.add(rect, port.toPointD())
                    }
                }
            }

            val node = graph.createNode()

            /**SbgnType Assignment */
            node.type = IOTypeMapper.getSbgnType(glyph)

            if (parent != null) {
                graph.setParent(node, idMap[parent.id] as INode)
            }
            else if (glyph.compartmentRef != null) {
                val compartmentNode = idMap[(glyph.compartmentRef as Glyph).id] as INode
                graph.setParent(node, compartmentNode)
            }

            graph.setIsGroupNode(node, node.type.isComplex() || node.type == SbgnType.COMPARTMENT)

            val isClone = glyph.clone != null
            node.setSbgnProperty(SbgnPropertyKey.IS_CLONE, isClone)
            idMap[glyph.id] = node

            node.graphStyle = getGraphStyle(glyph.extension)

            if (glyph.label?.text != null && glyph.isSupportingLabel()) {
                val label = graph.addLabel(node, glyph.label.text)
                label.type = SbgnType.NAME_LABEL
                graph.setNodeLayout(node, rect) //assign node size at this point to obtain correct label param
                SbgnBuilder.configure(graph, label, glyph.label?.bbox?.toRectD())
            }
            SbgnBuilder.configure(graph, node, rect)

            //take care. glyph.orientation defaults to "horizontal" even if it is not set at all.
            //this means we can't rely on it, when set to horizontal. hmmm, use some stupid heuristic
            if(node.type.isLogic() || node.type.isPN()) {
                if (glyph.orientation != "horizontal") node.orientation = "vertical"
                else node.orientation = getOrientationHeuristically(node)
            }
            else if(node.type == SbgnType.TAG) {
                if(glyph.orientation != "horizontal") node.orientation = glyph.orientation
                else node.orientation = "right" //heuristic based on port point would be better
            }

            glyph.port.forEach { port ->
                val newPort = graph.addPort(node, port.toPointD())
                idMap[port.id] = newPort
            }

            if (isClone && glyph.clone?.label?.text != null) {
                val cloneLabel = graph.addLabel(node, glyph.clone.label.text)
                cloneLabel.type = SbgnType.CLONE_LABEL
                SbgnBuilder.configure(graph, cloneLabel, glyph.clone.label.bbox?.toRectD())
            }

            if (glyph.glyph.any()) {
                processGlyphs(graph, idMap, glyph.glyph, glyph)
            }
        }

        processLabelGlyphs(graph, labelGlyphs, idMap, parent)

        for (glyph in portGlyphs) {
            if (glyph.ofClazz(GlyphClazz.TERMINAL)) {
                if (parent != null) {
                    if (glyph.bbox != null && glyph.orientation != null) {
                        val subMapNode = idMap[parent.id] as INode
                        val port = graph.addPort(subMapNode)
                        idMap[glyph.id] = port
                        val portBox = glyph.bbox.toRectD()

                        val portPoint = subMapNode.layout.convertToRatioPoint(when (glyph.orientation) {
                            "right" -> PointD(portBox.x, portBox.centerY)
                            "left" ->  PointD(portBox.maxX, portBox.centerY)
                            "up" -> PointD(portBox.centerX, portBox.maxY)
                            "down" -> PointD(portBox.centerX, portBox.y)
                            else -> PointD(portBox.x, portBox.centerY)
                        })
                        port.apply {
                            type = SbgnType.TERMINAL
                            setSbgnProperty(SbgnPropertyKey.ORIENTATION, glyph.orientation)
                            setSbgnProperty(SbgnPropertyKey.TERMINAL_SIZE, portBox.toSizeD())
                            setSbgnProperty(SbgnPropertyKey.TERMINAL_LABEL, glyph.label?.text)
                        }


                        SbgnBuilder.configure(graph, port, portPoint)
                    }
                } else {
                    println("found terminal without a parent")
                }
            }
        }
    }

    private fun getGraphStyle(ext: SBGNBase.Extension?):GraphStyle<SbgnType>? {
        return if(ext != null) {
            val styleElem = ext.any?.firstOrNull{ it.tagName == "style" && it.namespaceURI == krayonNS}
            styleElem?.attributes?.getNamedItem("idRef")?.nodeValue?.let { idStyleMap[it] }
        }
        else null
    }

    private fun getOrientationHeuristically(node:INode):String {
        var portBox = RectD.fromCenter(node.layout.center, SizeD(1.0,1.0))
        node.ports.forEach { portBox = RectD.add(portBox, it.location) }
        if(portBox.width > 1.0 || portBox.height > 1.0) {
            if(portBox.width > portBox.height) return "horizontal"
            else if(portBox.width < portBox.height) return "vertical"
        }
        //if that doesn't work: try use node size
        if(node.layout.width > node.layout.height) return "horizontal"
        else if(node.layout.width < node.layout.height) return "vertical"
        //if all fails use some default
        return "horizontal"
    }

    private fun processLabelGlyphs(graph: IGraph, labelGlyphs: List<Glyph>, idMap: HashMap<String, IModelItem>, parent: Glyph?) {
        for (glyph in labelGlyphs) {
            if (glyph.ofClazz(GlyphClazz.ANNOTATION)) {
                if (glyph.callout?.target != null) {
                    val node = idMap[(glyph.callout.target as Glyph).id] as INode
                    val text = glyph.label?.text ?: "<EMPTY>"
                    val label = graph.addLabel(node, text)
                    label.type = SbgnType.CALLOUT_LABEL
                    val labelBox = glyph.bbox?.toRectD()
                    val calloutPoint = if (glyph.callout.point != null) {
                        node.layout.convertToRatioPoint(glyph.callout.point.toPointD())
                    } else {
                        PointD.ORIGIN
                    }
                    label.setSbgnProperty(SbgnPropertyKey.CALLOUT_POINT, calloutPoint)
                    SbgnBuilder.configure(graph, label, labelBox)
                }
            } else if (glyph.ofClazz(GlyphClazz.UNIT_OF_INFORMATION)) {
                if (parent != null) {
                    val node = idMap[parent.id] as INode
                    val text = glyph.label?.text ?: "???"
                    val label = graph.addLabel(node, text)
                    label.type = SbgnType.UNIT_OF_INFORMATION
                    val labelBox = glyph.bbox?.toRectD()
                    SbgnBuilder.configure(graph, label, labelBox)
                }
            } else if (glyph.ofClazz(GlyphClazz.STATE_VARIABLE)) {
                if (parent != null) {
                    val node = idMap[parent.id] as INode
                    val text =
                            if (glyph.state?.variable != null)
                                (glyph.state?.value ?: "") + "@" + (glyph.state?.variable ?: "")
                            else
                                (glyph.state?.value ?: "")

                    val label = graph.addLabel(node, text)
                    label.type = SbgnType.STATE_VARIABLE
                    val labelBox = glyph.bbox?.toRectD()
                    SbgnBuilder.configure(graph, label, labelBox)
                }
            } else {
                println("Can't handle label type " + glyph.clazz)
            }
        }
    }
}

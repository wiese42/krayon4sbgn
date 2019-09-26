/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.*
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.labelmodels.*
import com.yworks.yfiles.graph.portlocationmodels.FreeNodePortLocationModel
import com.yworks.yfiles.graph.portlocationmodels.GenericPortLocationModel
import com.yworks.yfiles.view.Pen
import com.yworks.yfiles.view.TextAlignment
import com.yworks.yfiles.view.VerticalAlignment
import krayon.editor.base.model.IModelItemFeature
import krayon.editor.base.style.*
import krayon.editor.base.util.convertFromRatioPoint
import krayon.editor.base.util.scale
import krayon.editor.base.util.swap
import krayon.editor.sbgn.io.SbgnStyleIO
import krayon.editor.sbgn.model.*
import java.awt.Color
import java.awt.Font

object SbgnBuilder {

    val styleManager = StyleManager(typeMapper =  {
        when(it.type) {
            SbgnType.SIMPLE_CHEMICAL_MULTIMER -> SbgnType.SIMPLE_CHEMICAL
            SbgnType.MACROMOLECULE_MULTIMER -> SbgnType.MACROMOLECULE
            SbgnType.NUCLEIC_ACID_FEATURE_MULTIMER -> SbgnType.NUCLEIC_ACID_FEATURE
            SbgnType.COMPLEX_MULTIMER -> SbgnType.COMPLEX
            else -> it.type
        }},
        styleSetter = { item, graphStyle ->
            if(item is INode || item is IEdge) item.graphStyle = graphStyle
        }).apply {
        styleIO = SbgnStyleIO()
    }

    fun applyStyle(graph:IGraph, item:IModelItem) {
        (item.graphStyle ?:
        ((item as? ILabel)?.owner as? IModelItem)?.graphStyle ?:
        ((item as? IPort)?.owner as? IModelItem)?.graphStyle ?:
        ((item as? IModelItemFeature)?.owner)?.graphStyle ?:
        styleManager.currentStyle)?.let { style ->
            styleManager.applyStyle(style, graph, item, applySize = false)
        }
    }

    fun addPorts(graph:IGraph, node:INode) {
        val isHorizontal = node.orientation != "vertical"
        when {
            node.type.isPN() -> {
                if(isHorizontal) {
                    graph.addPort(node, FreeNodePortLocationModel.NODE_LEFT_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                    graph.addPort(node, FreeNodePortLocationModel.NODE_RIGHT_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                }
                else {
                    graph.addPort(node, FreeNodePortLocationModel.NODE_BOTTOM_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                    graph.addPort(node, FreeNodePortLocationModel.NODE_TOP_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                }
            }
            node.type.isLogic() -> {
                if(isHorizontal) {
                    graph.addPort(node, FreeNodePortLocationModel.NODE_LEFT_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                    graph.addPort(node, FreeNodePortLocationModel.NODE_RIGHT_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                }
                else {
                    graph.addPort(node, FreeNodePortLocationModel.NODE_BOTTOM_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                    graph.addPort(node, FreeNodePortLocationModel.NODE_TOP_ANCHORED).type = SbgnType.INPUT_AND_OUTPUT
                }
            }
        }
    }

    fun configure(graph: IGraph, label: ILabel, layout: IRectangle? = null) {
        when (label.type) {
            SbgnType.CALLOUT_LABEL -> {
                val node = label.owner as INode
                val delegateStyle = DefaultStyleableLabelStyle().apply { textAlignment = TextAlignment.CENTER; verticalTextAlignment = VerticalAlignment.CENTER }
                val calloutStyle = SbgnCalloutLabelStyle(delegateStyle)
                graph.setStyle(label, calloutStyle)

                if(layout != null) {
                    val param = AnchoredNodeLabelModel.INSTANCE.createParameter(node.layout, OrientedRectangle(layout))
                    graph.setLabelLayoutParameter(label, param)
                    graph.setLabelPreferredSize(label, layout.toSizeD())
                }
                else {
                    val param = getPreferredLabelParamForCalloutPoint(calloutStyle.calloutPoint)
                    graph.setLabelLayoutParameter(label, param)
                }
            }
            SbgnType.UNIT_OF_INFORMATION, SbgnType.STATE_VARIABLE -> {
                val node = label.owner as INode
                val shapeType = if(label.type == SbgnType.STATE_VARIABLE) SbgnAuxUnitLabelStyle.AuxUnitShape.ELLIPSE else SbgnAuxUnitLabelStyle.AuxUnitShape.RECTANGLE
                val insets = InsetsD.fromLTRB(10.0,6.0,7.0,7.0)
                graph.setStyle(label, SbgnAuxUnitLabelStyle(shapeType, insets).apply {
                    backgroundPen = Pen.getBlack()
                    backgroundPaint = Color.WHITE
                    font = this.font.deriveFont(Font.PLAIN, 10f)
                })
                val labelCenter = layout?.center ?: node.layout.convertFromRatioPoint(PointD(0.75,0.0))
                graph.adjustLabelPreferredSize(label)
                val model = AnchoredNodeLabelModel(OutlineConstraint())
                val param = model.findBestParameter(label, model, OrientedRectangle(RectD.fromCenter(labelCenter, label.preferredSize)))
                graph.setLabelLayoutParameter(label, param)
            }
            SbgnType.CARDINALITY -> {
                graph.setStyle(label, DefaultStyleableLabelStyle().apply {
                    backgroundPen = Pen.getBlack()
                    backgroundPaint = Color.WHITE
                    insets = InsetsD.fromLTRB(4.0,1.0,4.0,1.0)
                    font = font.deriveFont(Font.PLAIN, 10f)
                    textAlignment = TextAlignment.CENTER
                    verticalTextAlignment = VerticalAlignment.CENTER
                    isTextClippingEnabled = false
                })

                val model = EdgePathLabelModel(0.0, 0.0, 0.0, false, EdgeSides.LEFT_OF_EDGE.or(EdgeSides.RIGHT_OF_EDGE))
                if(layout != null) {
                    graph.setLabelPreferredSize(label, layout.toSizeD())
                    graph.setLabelLayoutParameter(label, model.findBestParameter(label, model, OrientedRectangle(layout)))
                }
                else {
                    graph.setLabelLayoutParameter(label, model.createRatioParameter(0.5, EdgeSides.LEFT_OF_EDGE))
                }
            }
            SbgnType.NAME_LABEL -> {
                val node = label.owner as INode
                when(node.type) {
                    SbgnType.COMPARTMENT -> {
                        graph.setStyle(label, DefaultStyleableLabelStyle().apply { isTextClippingEnabled = false; font = this.font.deriveFont(Font.PLAIN, 16f)})
                        graph.adjustLabelPreferredSize(label)
                        val model = AnchoredNodeLabelModel(InteriorConstraint())
                        val param = if(layout != null) {
                            model.findBestParameter(label, model, OrientedRectangle(RectD.fromCenter(layout.center, label.preferredSize)))
                        }
                        else {
                            model.createParameter(InteriorLabelModel.Position.NORTH, InsetsD.fromLTRB(0.0,5.0,0.0,0.0))
                        }
                        graph.setLabelLayoutParameter(label, param)
                    }
                    SbgnType.TAG -> {
                        graph.setStyle(label, DefaultStyleableLabelStyle().apply {
                            textAlignment = TextAlignment.CENTER; verticalTextAlignment = VerticalAlignment.CENTER; isTextClippingEnabled = false
                            font = font.deriveFont(Font.PLAIN, 12f)
                        })
                        val model = AnchoredNodeLabelModel(InteriorConstraint())
                        val param = if(layout != null) {
                            model.findBestParameter(label, model, OrientedRectangle(RectD.fromCenter(layout.center, label.preferredSize)))
                        }
                        else {
                            val hOffset = Math.min(node.layout.height / Math.sqrt(2.0), node.layout.width * 0.5) * 0.5
                            val vOffset = Math.min(node.layout.width / Math.sqrt(2.0), node.layout.height * 0.5) * 0.5
                            val inset = when(node.orientation) {
                                "right" -> InsetsD.fromLTRB(0.0,0.0,hOffset,0.0)
                                "left" -> InsetsD.fromLTRB(hOffset,0.0,0.0,0.0)
                                "up" -> InsetsD.fromLTRB(0.0,vOffset,0.0,0.0)
                                "down" -> InsetsD.fromLTRB(0.0,0.0,0.0,vOffset)
                                else -> InsetsD.EMPTY
                            }
                            model.createParameter(InteriorLabelModel.Position.CENTER, inset)
                        }
                        graph.setLabelLayoutParameter(label, param)
                    }
                    else -> {
                        graph.setStyle(label, DefaultStyleableLabelStyle().apply {
                            textAlignment = TextAlignment.CENTER; verticalTextAlignment = VerticalAlignment.CENTER; isTextClippingEnabled = false
                            font = font.deriveFont(Font.PLAIN, 12f)
                        })
                        val model = AnchoredNodeLabelModel(InteriorConstraint())
                        val param = if(layout != null) {
                            model.findBestParameter(label, model, OrientedRectangle(RectD.fromCenter(layout.center, label.preferredSize)))
                        }
                        else {
                            val labelPos = if(node.type.isComplex()) InteriorLabelModel.Position.NORTH else InteriorLabelModel.Position.CENTER
                            model.createParameter(labelPos, InsetsD(5.0))
                        }
                        graph.setLabelLayoutParameter(label, param)
                    }
                }
            }

            SbgnType.CLONE_LABEL -> {
                val node = label.owner as INode
                graph.setStyle(label, CloneMarkerLabelStyle().apply {
                    textAlignment = TextAlignment.CENTER; verticalTextAlignment = VerticalAlignment.CENTER; isTextClippingEnabled = false
                    font = this.font.deriveFont(Font.PLAIN, 10f)
                    textPaint = Color.WHITE
                })

                if(layout != null) {
                    graph.setLabelPreferredSize(label, layout.toSizeD())
                    graph.setLabelLayoutParameter(label,
                            RatioNodeLabelModel.INSTANCE.createParameter(node.layout, OrientedRectangle(layout)))
                }
                else {
                    if(label.text.isEmpty()) {
                        graph.setLabelPreferredSize(label, node.layout.toSizeD().scale(0.5,0.2))
                    }
                    graph.setLabelLayoutParameter(label,
                            RatioNodeLabelModel.INSTANCE.createParameter(PointD(0.5, 0.84)))
                }
            }
            else -> {
                println("not supported yet")
            }
        }

        applyStyle(graph, label)
    }

    fun configure(graph: IGraph, edge:IEdge) {
        graph.setStyle(edge, SbgnPolylineEdgeStyle().apply {
            targetArrow = SbgnArrows.create(edge.type)
        })
        applyStyle(graph, edge)
    }

    fun configure(graph: IGraph, node: INode, layout:IRectangle? = null) {
        var size = SizeD(100.0,60.0)

        if(node.type.isPN()) {
            if(node.orientation == null) node.orientation = "horizontal"
        }
        else if(node.type.isLogic()) {
            if(node.orientation == null) node.orientation = "vertical"
        }

        when(node.type) {
            SbgnType.SIMPLE_CHEMICAL, SbgnType.SIMPLE_CHEMICAL_MULTIMER -> {
                size = SizeD(60.0,60.0)
                graph.setStyle(node, SbgnSimpleChemicalStyle())
            }
            SbgnType.MACROMOLECULE, SbgnType.MACROMOLECULE_MULTIMER -> {
                graph.setStyle(node, SbgnMacroMoleculeStyle())
            }
            SbgnType.NUCLEIC_ACID_FEATURE, SbgnType.NUCLEIC_ACID_FEATURE_MULTIMER -> {
                graph.setStyle(node, SbgnNucleicAcidFeatureStyle())
            }
            SbgnType.COMPLEX, SbgnType.COMPLEX_MULTIMER -> {
                size = SizeD(160.0,160.0)
                graph.setStyle(node, SbgnComplexStyle())
            }
            SbgnType.TAG -> {
                if(node.orientation == null) node.orientation = "right"
                graph.setStyle(node, SbgnTagStyle())
            }
            SbgnType.SOURCE_AND_SINK -> {
                size = SizeD(60.0,60.0)
                graph.setStyle(node, SbgnSourceAndSinkStyle())
            }
            SbgnType.PROCESS -> {
                size = if(node.orientation == "vertical") SizeD(20.0,40.0) else SizeD(40.0,20.0)
                graph.setStyle(node, SbgnProcessStyle(SbgnProcessStyle.Type.DEFAULT))
            }
            SbgnType.OMITTED_PROCESS -> {
                size = if(node.orientation == "vertical") SizeD(20.0,40.0) else SizeD(40.0,20.0)
                graph.setStyle(node, SbgnProcessStyle(SbgnProcessStyle.Type.OMITTED))
            }
            SbgnType.UNCERTAIN_PROCESS -> {
                size = if(node.orientation == "vertical") SizeD(20.0,40.0) else SizeD(40.0,20.0)
                graph.setStyle(node, SbgnProcessStyle(SbgnProcessStyle.Type.UNCERTAIN))
            }
            SbgnType.DISSOCIATION -> {
                size = if(node.orientation == "vertical") SizeD(20.0,40.0) else SizeD(40.0,20.0)
                graph.setStyle(node, SbgnProcessStyle(SbgnProcessStyle.Type.DISSOCIATION))
            }
            SbgnType.ASSOCIATION -> {
                size = if(node.orientation == "vertical") SizeD(20.0,40.0) else SizeD(40.0,20.0)
                graph.setStyle(node, SbgnProcessStyle(SbgnProcessStyle.Type.ASSOCIATION))
            }
            SbgnType.PHENOTYPE -> {
                graph.setStyle(node, SbgnPhenotypeStyle())
            }
            SbgnType.OR -> {
                size = if(node.orientation == "vertical") SizeD(30.0,50.0) else SizeD(50.0,30.0)
                graph.setStyle(node, SbgnLogicalOperatorStyle(SbgnLogicalOperatorStyle.Type.OR))
            }
            SbgnType.AND -> {
                size = if(node.orientation == "vertical") SizeD(30.0,50.0) else SizeD(50.0,30.0)
                graph.setStyle(node, SbgnLogicalOperatorStyle(SbgnLogicalOperatorStyle.Type.AND))
            }
            SbgnType.NOT -> {
                size = if(node.orientation == "vertical") SizeD(30.0,50.0) else SizeD(50.0,30.0)
                graph.setStyle(node, SbgnLogicalOperatorStyle(SbgnLogicalOperatorStyle.Type.NOT))
            }
            SbgnType.COMPARTMENT -> {
                size = SizeD(160.0,120.0)
                graph.setStyle(node, SbgnCompartmentStyle())
            }
            SbgnType.SUBMAP -> {
                size = SizeD(150.0,90.0)
                graph.setStyle(node, SbgnSubmapStyle())
            }
            SbgnType.UNSPECIFIED_ENTITY -> {
                graph.setStyle(node, SbgnUnspecifiedEntityStyle())
            }
            SbgnType.PERTURBING_AGENT -> {
                graph.setStyle(node, SbgnPerturbingAgentStyle())
            }
            SbgnType.ANNOTATION -> {
                graph.setStyle(node, AnnotationNodeStyle().apply {
                    showTipHandle = false
                    tipAnchor = PointD(0.5, 0.5)
                    topLeftBoxAnchor = PointD.ORIGIN
                    bottomRightBoxAnchor = PointD(1.0,1.0)
                })
            }
            else -> {}
        }

        graph.setNodeLayout(node, layout?.toRectD() ?: RectD(PointD.ORIGIN, size))

        applyStyle(graph, node)
    }

    fun configure(graph: IGraph, port: IPort, location: PointD) {
        when(port.type) {
            SbgnType.TERMINAL -> {
                val genericModel = GenericPortLocationModel()
                for(i in 1..9) {
                    genericModel.addParameter(FreeNodePortLocationModel.INSTANCE.createParameter(PointD(0.1*i,0.0)))
                    genericModel.addParameter(FreeNodePortLocationModel.INSTANCE.createParameter(PointD(0.1*i,1.0)))
                    genericModel.addParameter(FreeNodePortLocationModel.INSTANCE.createParameter(PointD(0.0,0.1*i)))
                    genericModel.addParameter(FreeNodePortLocationModel.INSTANCE.createParameter(PointD(1.0,0.1*i)))
                }
                graph.setPortLocationParameter(port, genericModel.addParameter(FreeNodePortLocationModel.INSTANCE.createParameter(location)))
                graph.setStyle(port, SbgnTerminalPortStyle())
            }
            else -> {}
        }
        applyStyle(graph, port)
    }

    fun addNameLabel(graph: IGraph, node: INode):ILabel {
        val label = graph.addLabel(node, "")
        label.type = SbgnType.NAME_LABEL
        graph.setLabelText(label, SbgnBuilder.getDefaultLabelText(label))
        SbgnBuilder.configure(graph, label)
        return label
    }

    fun assignType(graph: IGraph, node: INode, type: SbgnType) {
        node.type = type
        val center = node.layout.center.toPointD()
        SbgnBuilder.configure(graph,node)
        graph.setNodeLayout(node, RectD.fromCenter(center, node.layout.toSizeD()))
    }

    fun reverseEdgeStyle(edge: IEdge) {
        val style = edge.style as SbgnPolylineEdgeStyle
        val tmp = style.sourceArrow
        style.sourceArrow = style.targetArrow
        style.targetArrow = tmp
    }


    /**
     * @param calloutPoint node relative ratio point
     */
    fun getPreferredLabelParamForCalloutPoint(calloutPoint: PointD):ILabelModelParameter {
        val externalPos = when {
            calloutPoint.x < 0.5 && calloutPoint.y < 0.5 -> ExteriorLabelModel.Position.NORTH_WEST
            calloutPoint.x < 0.5 && calloutPoint.y == 0.5 -> ExteriorLabelModel.Position.WEST
            calloutPoint.x < 0.5 && calloutPoint.y > 0.5 -> ExteriorLabelModel.Position.SOUTH_WEST
            calloutPoint.x > 0.5 && calloutPoint.y < 0.5 -> ExteriorLabelModel.Position.NORTH_EAST
            calloutPoint.x > 0.5 && calloutPoint.y == 0.5 -> ExteriorLabelModel.Position.EAST
            calloutPoint.x > 0.5 && calloutPoint.y > 0.5 -> ExteriorLabelModel.Position.SOUTH_EAST
            calloutPoint.x == 0.5 && calloutPoint.y < 0.5 -> ExteriorLabelModel.Position.NORTH
            calloutPoint.x == 0.5 && calloutPoint.y > 0.5 -> ExteriorLabelModel.Position.SOUTH
            else -> ExteriorLabelModel.Position.NORTH_WEST
        }
        return AnchoredNodeLabelModel.INSTANCE.createParameter(externalPos, InsetsD.fromLTRB(-10.0,10.0,-10.0,10.0))
    }

    private fun applyStyleToLabel(context:IStyleableContext, map:Map<StyleProperty, Any?>, labelType:SbgnType) {
        val node = context.item as? INode
        if(node != null) {
            for (label in node.labels) {
                if (label.type == labelType) {
                    (label.style as? IStyleable)?.applyStyle(DefaultStyleableContext(label, context.graph, context.graphComponent), map)
                    return
                }
            }
        }
    }
    fun applyStyleToNameLabel(context:IStyleableContext, map:Map<StyleProperty, Any?>) = applyStyleToLabel(context, map, SbgnType.NAME_LABEL)
    fun applyStyleToCloneMarkerLabel(context:IStyleableContext, map:Map<StyleProperty, Any?>) = applyStyleToLabel(context, map, SbgnType.CLONE_LABEL)

    private fun retrieveStyleFromLabel(context:IStyleableContext, map:MutableMap<StyleProperty, Any?>, labelType:SbgnType):Boolean {
        val node = context.item as? INode
        if(node != null) {
            for (label in node.labels) {
                if(label.type == labelType) {
                    (label.style as? IStyleable)?.retrieveStyle(DefaultStyleableContext(label, context.graph, context.graphComponent), map)
                    return true
                }
            }
        }
        return false
    }
    fun retrieveStyleFromNameLabel(context:IStyleableContext, map:MutableMap<StyleProperty, Any?>) = retrieveStyleFromLabel(context, map, SbgnType.NAME_LABEL)
    fun retrieveStyleFromCloneMarkerLabel(context:IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        if(!retrieveStyleFromLabel(context, map, SbgnType.CLONE_LABEL)) {
            map[StyleProperty.CloneMarkerFontSize] = 10.0
            map[StyleProperty.CloneMarkerTextColor] = Color.WHITE
            map[StyleProperty.CloneMarkerFontStyle] =   FontStyleValue.Plain
        }
    }

    fun getDefaultLabelText(label: ILabel): String {
        return when(label.type) {
            SbgnType.NAME_LABEL -> "Label"
            SbgnType.CALLOUT_LABEL -> "Info"
            else -> ""
        }
    }

    fun configure(graph: IGraph, feature: IModelItemFeature) {
        if(feature.type == SbgnType.CLONE_MARKER) {
            val node = feature.owner as? INode
            if(node != null) {
                feature.style = CloneMarkerFeatureStyleable()
                node.type = SbgnType.SIMPLE_CHEMICAL //necessary???
                node.isClone = true
                val style = NodeStyleForCloneMarkerFeature().apply {
                    pen = Pen.getLightGray()
                    paint = Color(0,0,0,0) // transparent
                }
                graph.setStyle(node, style)
            }
        }
        else if(feature.type == SbgnType.MULTIMER) {
            val node = feature.owner as? INode
            if(node != null) {
                node.type = SbgnType.MACROMOLECULE_MULTIMER //necessary???
                val style = NodeStyleForMultimerFeature().apply {
                    pen = Pen.getLightGray()
                    paint = Color(0,0,0,0) // transparent
                }
                graph.setStyle(node, style)
            }
        }
    }

    private fun getPrevalentSizeCore(graph: IGraph, type: SbgnType, orientation:String?, excludedNode:INode? = null): SizeD? {
        var relevantNodes = graph.nodes.filter { it != excludedNode && it.type == type && it.orientation == orientation }
        if (relevantNodes.isEmpty()) {
            relevantNodes = graph.nodes.filter {
                when {
                    type.isLogic() -> it.type.isLogic() && it.orientation == orientation
                    type.isPN() -> it.type.isPN() && it.orientation == orientation
                    type.isSimpleChemical() -> it.type.isSimpleChemical()
                    type.isMacromolecule() -> it.type.isMacromolecule()
                    type.isNucleicAcidFeature() -> it.type.isNucleicAcidFeature()
                    else -> false
                }
            }
        }

        return if (relevantNodes.any{ it != excludedNode}) {
            val heightHistogram = relevantNodes.groupBy { it.layout.height }
            val widthHistogram = relevantNodes.groupBy { it.layout.width }
            val sizeHistogram = relevantNodes.groupBy { it.layout.toSizeD() }
            //println("height=" + heightHistogram.size + "  width=" + widthHistogram.size + " size=" + sizeHistogram.size)
            when {
                sizeHistogram.size == 1 -> relevantNodes.first().layout.toSizeD()
                heightHistogram.size == 1 -> //uniform height. choose smallest most prevalent width
                    heightHistogram.values.first().minBy { it.layout.width }?.layout?.toSizeD()
                widthHistogram.size == 1 -> //uniform width. choose smallest most prevalent height
                    widthHistogram.values.first().minBy { it.layout.height }?.layout?.toSizeD()
                else -> null
            }
        }
        else null
    }

    fun getPrevalentSize(graph: IGraph, type: SbgnType, orientation:String?, excludedNode:INode? = null): SizeD? {
        return getPrevalentSizeCore(graph, type, orientation, excludedNode) ?: when (orientation) {
            "horizontal" -> getPrevalentSizeCore(graph, type, "vertical", excludedNode)?.swap()
            "vertical" -> getPrevalentSizeCore(graph, type, "horizontal", excludedNode)?.swap()
            else -> null
        }
    }
}

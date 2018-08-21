/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.InsetsD
import com.yworks.yfiles.geometry.MutableRectangle
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.graph.DefaultGraph
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.INode
import krayon.editor.base.util.center
import krayon.editor.base.util.enlargeTLRB
import krayon.editor.base.util.translate
import krayon.editor.base.util.unaryMinus
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.getNameLabel
import krayon.editor.sbgn.model.type
import kotlin.math.max
import kotlin.math.min

class BricksPaletteComponent(modelGraph:IGraph = DefaultGraph()) : SbgnPaletteComponent(modelGraph) {

    private var arcDist = 46.0
    private var gapDist = 10.0
    private var borderDist = 10.0
    private val idBrickMap = mutableMapOf<String, IGraph>()

    init {
        configureGraphPalette()
    }

    private fun configureGraphPalette() {

    }

    fun addBrick(graph:IGraph, id:String) {
        idBrickMap[id] = graph
        addPaletteGraph(graph)
    }

    override fun invalidateRenderer() {
        layoutBricks()
        super.invalidateRenderer()
    }

    @Suppress("NestedLambdaShadowedImplicitParameter")
    fun layoutBricks() {
        idBrickMap.forEach { id, graph ->
            when(id) {
                "REACTION_IRR_1_1" -> layoutReaction(graph)
                "CATALYSIS_IRR_1_1","CATALYSIS_REV_1_1" -> {
                    layoutReaction(graph)
                    layoutEnzyme(graph)
                }
                "CATALYSIS_IRR_2_2" -> {
                    layoutReaction(graph)
                    layoutEnzyme(graph)
                    layoutExtraReactants(graph)
                }
                "CATALYSIS_2_REV_1_1" -> {
                    layoutTwoWayCatalysis(graph)
                }
                "INHIBITION_IRR_1_1" -> {
                    layoutReaction(graph)
                    layoutEnzyme(graph)
                    layoutInhibitor(graph)
                }
                "PHOSPHORYLATION_2_2" -> {
                    val s1 = graph.nodes.first { it.getNameLabel()?.text == "S1" }
                    val p1 = graph.nodes.first { it.getNameLabel()?.text == "P1" }
                    val s2 = graph.nodes.first { it.getNameLabel()?.text == "ATP" }
                    val p2 = graph.nodes.first { it.getNameLabel()?.text == "ADP" }
                    val enzyme = graph.nodes.first { it.getNameLabel()?.text == "kinase" }
                    layoutReaction(graph,s1,p1)
                    layoutEnzyme(graph, enzyme)
                    layoutExtraReactants(graph,s1,p1, s2, p2)
                }
                "PROTEIN_PHOSPHORYLATION_1_1" -> {
                    val s1 = graph.nodes.first { it.getNameLabel()?.text == "X" && it.labels.none { it.text == "P" } }
                    val p1 = graph.nodes.first { it.getNameLabel()?.text == "X" && it.labels.any { it.text == "P" } }
                    layoutReaction(graph,s1,p1)
                    val enzyme = graph.nodes.first { it.getNameLabel()?.text == "kinase" }
                    layoutEnzyme(graph, enzyme)
                }
                "PROTEIN_PHOSPHORYLATION_2_2" -> {
                    val s1 = graph.nodes.first { it.getNameLabel()?.text == "X" && it.labels.none { it.text == "P" } }
                    val p1 = graph.nodes.first { it.getNameLabel()?.text == "X" && it.labels.any { it.text == "P" } }
                    val s2 = graph.nodes.first { it.getNameLabel()?.text == "ATP" }
                    val p2 = graph.nodes.first { it.getNameLabel()?.text == "ADP" }
                    val enzyme = graph.nodes.first { it.getNameLabel()?.text == "kinase" }
                    layoutReaction(graph,s1,p1)
                    layoutEnzyme(graph, enzyme)
                    layoutExtraReactants(graph,s1,p1, s2, p2)
                }
                "OLIGOMERISATION" -> {
                    val s1 = graph.nodes.first { it.getNameLabel()?.text == "X" && it.labels.none { it.text.startsWith("N:") } }
                    val p1 = graph.nodes.first { it.getNameLabel()?.text == "X" && it.labels.any { it.text.startsWith("N:") } }
                    layoutReaction(graph,s1,p1)
                }
                "COMPLEX_ASSOCIATION" -> {
                    val cx = graph.nodes.first{ it.getNameLabel()?.text == "X" && graph.getParent(it)?.type?.isComplex() == true}
                    val cy = graph.nodes.first{ it.getNameLabel()?.text == "Y" && graph.getParent(it)?.type?.isComplex() == true}
                    val x = graph.nodes.first{ it.getNameLabel()?.text == "X" && graph.getParent(it)?.type?.isComplex() != true}
                    val y = graph.nodes.first{ it.getNameLabel()?.text == "Y" && graph.getParent(it)?.type?.isComplex() != true}
                    val process = graph.nodes.first{ it.type == SbgnType.ASSOCIATION }
                    layoutComplexAssociation(graph, x,y,process, cx, cy)
                }
                "COMPLEX_DISSOCIATION" -> {
                    layoutComplexDissociation(graph)
                }
                "TRANSCRIPTION" -> {
                    val cx = graph.nodes.first{ it.getNameLabel()?.text == "TF" && graph.getParent(it)?.type?.isComplex() == true}
                    val cy = graph.nodes.first{ it.getNameLabel()?.text == "X" && graph.getParent(it)?.type?.isComplex() == true}
                    val x = graph.nodes.first{ it.getNameLabel()?.text == "TF" && graph.getParent(it)?.type?.isComplex() != true}
                    val y = graph.nodes.first{ it.getNameLabel()?.text == "X" && graph.getParent(it)?.type?.isComplex() != true}
                    val process = graph.nodes.first{ it.type == SbgnType.ASSOCIATION }
                    layoutComplexAssociation(graph, x,y,process, cx, cy)
                    val complex = graph.getParent(cx)
                    layoutRegulatedProcess(graph, complex)
                }
                "TRANSLATION" -> {
                    val regulator = graph.nodes.first { it.labels.any { it.text.endsWith("gene") }}
                    graph.setNodeCenter(regulator, PointD.ORIGIN)
                    layoutRegulatedProcess(graph, regulator)
                }
                "PASSIVE_TRANSPORT" -> {
                    val process = graph.nodes.first { it.type.isPN()}
                    val s1 = graph.inEdgesAt(process).first().sourceNode
                    val p1 = graph.outEdgesAt(process).first().targetNode
                    layoutReaction(graph,s1,p1,process)
                    val nucleus = graph.nodes.first { it.getNameLabel()?.text == "NUCLEUS"}
                    graph.setNodeLayout(nucleus,
                            graph.groupingSupport.calculateMinimumEnclosedArea(nucleus).getEnlarged(InsetsD.fromLTRB(arcDist*0.5,
                                    borderDist+nucleus.getNameLabel()!!.layout.height, borderDist, borderDist)))

                    val cytosol = graph.nodes.first { it.getNameLabel()?.text == "CYTOSOL"}
                    val box = MutableRectangle(nucleus.layout).apply {
                        add(process.layout)
                        add(s1.layout)
                        enlargeTLRB(borderDist+cytosol.getNameLabel()!!.layout.height, borderDist, borderDist, borderDist)
                    }
                    graph.setNodeLayout(cytosol, box.toRectD())
                }
                "ACTIVE_TRANSPORT" -> {
                    val process = graph.nodes.first { it.type.isPN()}
                    val s1 = graph.inEdgesAt(process).first().sourceNode
                    val regulator = graph.nodes.first { it.getNameLabel()?.text == "Y" }
                    graph.setNodeCenter(regulator, PointD.ORIGIN)
                    layoutRegulatedProcess(graph, regulator)
                    val nucleus = graph.nodes.first { it.getNameLabel()?.text == "NUCLEUS"}
                    graph.setNodeLayout(nucleus, graph.groupingSupport.calculateMinimumEnclosedArea(nucleus).getEnlarged(
                            InsetsD.fromLTRB(arcDist*0.5,borderDist+nucleus.getNameLabel()!!.layout.height, borderDist, borderDist)))
                    val membrane = graph.nodes.first { it.getNameLabel()?.text == "NUCLEAR MEMBRANE" }
                    val mbox = MutableRectangle(nucleus.layout).apply {
                        add(process.layout)
                        add(regulator.layout)
                        enlargeTLRB(borderDist+membrane.getNameLabel()!!.layout.height, borderDist, borderDist, borderDist)
                    }
                    graph.setNodeLayout(membrane, mbox.toRectD())
                    val cytosol = graph.nodes.first { it.getNameLabel()?.text == "CYTOSOL"}
                    val cbox = MutableRectangle(membrane.layout).apply {
                        add(s1.layout)
                        enlargeTLRB(borderDist+cytosol.getNameLabel()!!.layout.height, borderDist, borderDist, borderDist)
                    }
                    graph.setNodeLayout(cytosol, cbox.toRectD())
                }
            }

            val zoom = 1.0
            val graphComponent = graphRendererProvider.invoke(graph)
            graphComponent.updateContentRect()
            val contentRect = graphComponent.contentRect
            graph.translate(-contentRect.toPointD())
            val nodeBox = RectD(contentRect.x, contentRect.y, contentRect.width * zoom, contentRect.height * zoom)
            val node = modelGraph.nodes.first{ it.tag == graph}
            modelGraph.setNodeLayout(node, nodeBox)
        }
    }

    private fun layoutTwoWayCatalysis(graph: IGraph) {
        val s = graph.nodes.find { it.getNameLabel()?.text == "S1" }!!
        graph.setNodeCenter(s, PointD.ORIGIN)
        val p1 = graph.outEdgesAt(s).first().targetNode
        val p2 = graph.inEdgesAt(s).first().sourceNode
        val processDist = gapDist*0.5
        graph.setNodeCenter(p1, PointD(s.layout.maxX+arcDist+p1.layout.width*0.5, s.center.y-processDist*0.5-p1.layout.height*0.5))
        graph.setNodeCenter(p2, PointD(p1.center.x, p1.layout.maxY+processDist+p2.layout.height*0.5))

        val p = graph.outEdgesAt(p1).first().targetNode
        graph.setNodeCenter(p, PointD(p1.layout.maxX + arcDist + p.layout.width*0.5, s.center.y))
        val e1 = graph.inEdgesAt(p1).first { it.type.isRegulation() }.sourceNode
        val e2 = graph.inEdgesAt(p2).first { it.type.isRegulation() }.sourceNode
        graph.setNodeCenter(e1, PointD(p1.center.x, p1.layout.y - arcDist - e1.layout.height*0.5))
        graph.setNodeCenter(e2, PointD(p2.center.x, p2.layout.maxY + arcDist + e2.layout.height*0.5))

        s.ports.first { graph.outDegree(it) == 1}.let {
            graph.setPortLocation(it, PointD(s.center.x, max(p1.center.y, s.layout.y)))
        }
        s.ports.first { graph.inDegree(it) == 1}.let {
            graph.setPortLocation(it, PointD(s.center.x, max(p2.center.y, s.layout.y)))
        }
        p.ports.first { graph.inDegree(it) == 1}.let {
            graph.setPortLocation(it, PointD(p.center.x, min(p1.center.y, p.layout.maxY)))
        }
        p.ports.first { graph.outDegree(it) == 1}.let {
            graph.setPortLocation(it, PointD(p.center.x, min(p2.center.y, p.layout.maxY)))
        }
    }

    private fun layoutRegulatedProcess(graph:IGraph, regulator:INode) {
        val process = graph.outEdgesAt(regulator).first().targetNode
        val educt = graph.inEdgesAt(process).first().sourceNode
        val product = graph.outEdgesAt(process).first().targetNode
        graph.setNodeCenter(process, PointD(regulator.center.x, regulator.layout.maxY+arcDist+process.layout.height*0.5))
        graph.setNodeCenter(educt, PointD(process.layout.x-arcDist-educt.layout.width*0.5, process.center.y))
        graph.setNodeCenter(product, PointD(process.layout.maxX+arcDist+product.layout.width*0.5, process.center.y))
    }

    private fun layoutComplexDissociation(graph:IGraph) {
        val cx = graph.nodes.first{ it.getNameLabel()?.text == "X" && graph.getParent(it)?.type?.isComplex() == true}
        val cy = graph.nodes.first{ it.getNameLabel()?.text == "Y" && graph.getParent(it)?.type?.isComplex() == true}
        val complex = graph.nodes.first{ it.type.isComplex()}
        val x = graph.nodes.first{ it.getNameLabel()?.text == "X" && graph.getParent(it)?.type?.isComplex() != true}
        val y = graph.nodes.first{ it.getNameLabel()?.text == "Y" && graph.getParent(it)?.type?.isComplex() != true}
        val process = graph.nodes.first{ it.type.isPN() }
        graph.setNodeCenter(cx, PointD.ORIGIN)
        graph.setNodeCenter(cy, PointD(cx.center.x, cx.layout.maxY+gapDist+cy.layout.height*0.5))
        val complexMin = PointD(cx.layout.x-gapDist, cx.layout.y-gapDist)
        val complexMax = PointD(cy.layout.maxX+gapDist, cy.layout.maxY+gapDist)
        graph.setNodeLayout(complex, RectD(complexMin, complexMax))
        graph.setNodeCenter(process, PointD(complex.layout.maxX+arcDist-borderDist+process.layout.width*0.5, complex.center.y))
        graph.setNodeCenter(x, PointD(process.layout.maxX+arcDist-borderDist+x.layout.width*0.5, complex.layout.y+x.layout.height*0.5))
        graph.setNodeCenter(y, PointD(process.layout.maxX+arcDist-borderDist+y.layout.width*0.5, complex.layout.maxY-y.layout.height*0.5))
    }

    private fun layoutComplexAssociation(graph:IGraph, x:INode, y:INode, process:INode, cx:INode, cy:INode) {
        val complex = graph.getParent(cx)
        graph.setNodeCenter(cx, PointD.ORIGIN)
        graph.setNodeCenter(cy, PointD(cx.center.x, cx.layout.maxY+gapDist+cy.layout.height*0.5))
        val complexMin = PointD(min(cx.layout.x, cy.layout.x)-gapDist, cx.layout.y-gapDist)
        val complexMax = PointD(max(cx.layout.maxX, cy.layout.maxX) +gapDist, cy.layout.maxY+gapDist)
        graph.setNodeLayout(complex, RectD(complexMin, complexMax))
        graph.setNodeCenter(process, PointD(complex.layout.x-arcDist+borderDist-process.layout.width*0.5, complex.center.y))

        val maxWidth = max(x.layout.width, y.layout.width)
        graph.setNodeCenter(x, PointD(process.layout.x-arcDist+borderDist-maxWidth*0.5, complex.layout.y+x.layout.height*0.5))
        graph.setNodeCenter(y, PointD(process.layout.x-arcDist+borderDist-maxWidth*0.5, complex.layout.maxY-y.layout.height*0.5))
    }

    private fun layoutReaction(graph:IGraph,
                               s1: INode = graph.nodes.first{ it.getNameLabel()?.text == "S1"},
                               p1: INode = graph.nodes.first{ it.getNameLabel()?.text == "P1"},
                               process: INode = graph.nodes.first{ it.type.isPN() }) {
        graph.setNodeCenter(s1, PointD.ORIGIN)
        graph.setNodeCenter(process, PointD(s1.layout.maxX+arcDist+process.layout.width * 0.5, 0.0))
        graph.setNodeCenter(p1, PointD(process.layout.maxX+arcDist+p1.layout.width * 0.5, 0.0))
    }

    private fun layoutEnzyme(graph:IGraph, enzyme:INode = graph.nodes.first{ it.getNameLabel()?.text == "enzyme"} ) {
        val process = graph.nodes.first{ it.type == SbgnType.PROCESS }
        graph.setNodeCenter(enzyme, PointD(process.center.x, process.layout.y - arcDist - enzyme.layout.height * 0.5))
    }

    private fun layoutInhibitor(graph:IGraph) {
        val inhibitor = graph.nodes.first{ it.getNameLabel()?.text == "inhibitor"}
        val process = graph.nodes.first{ it.type == SbgnType.PROCESS }
        graph.setNodeCenter(inhibitor, PointD(process.center.x, process.layout.maxY + arcDist + inhibitor.layout.height * 0.5))
    }

    private fun layoutExtraReactants(graph:IGraph,
                                     s1:INode = graph.nodes.first{ it.getNameLabel()?.text == "S1"},
                                     p1:INode = graph.nodes.first{ it.getNameLabel()?.text == "P1"},
                                     s2:INode = graph.nodes.first{ it.getNameLabel()?.text == "S2"},
                                     p2:INode = graph.nodes.first{ it.getNameLabel()?.text == "P2"}) {
        val process = graph.nodes.first{ it.type == SbgnType.PROCESS }
        graph.setNodeCenter(s2, PointD((process.center.x + s1.center.x)*0.5, s1.layout.maxY+s2.layout.height*0.5+gapDist))
        graph.setNodeCenter(p2, PointD((process.center.x + p1.center.x)*0.5, p1.layout.maxY+p2.layout.height*0.5+gapDist))
    }
}
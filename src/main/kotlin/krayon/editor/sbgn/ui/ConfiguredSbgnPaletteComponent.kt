/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.*
import krayon.editor.base.model.IModelItemFeature
import krayon.editor.base.style.IStyleable
import krayon.editor.base.style.StyleProperty
import krayon.editor.base.style.StyleTemplateMap
import krayon.editor.base.style.retrieveStyle
import krayon.editor.sbgn.model.*
import krayon.editor.sbgn.style.SbgnBuilder

class ConfiguredSbgnPaletteComponent(modelGraph:IGraph = DefaultGraph()) : SbgnPaletteComponent(modelGraph) {
    
    init {
        configureGraphPalette()
    }

    private fun configureGraphPalette() {

        addSection() //"Nodes")

        listOf(SbgnType.SIMPLE_CHEMICAL, SbgnType.MACROMOLECULE, SbgnType.NUCLEIC_ACID_FEATURE, SbgnType.COMPLEX).forEach { type ->
            addPaletteNode{ node, graph ->
                node.type = type
                if(type.isComplex()) graph.setIsGroupNode(node, true)
                SbgnBuilder.configure(graph, node)
                SbgnBuilder.addNameLabel(graph, node)
            }
        }

        listOf(SbgnType.UNSPECIFIED_ENTITY, SbgnType.PERTURBING_AGENT).forEach { type ->
            addPaletteNode{ node, graph ->
                node.type = type
                SbgnBuilder.configure(graph, node)
                SbgnBuilder.addNameLabel(graph, node)
            }
        }

        addPaletteNode{ node, graph ->
            node.type = SbgnType.SOURCE_AND_SINK
            SbgnBuilder.configure(graph, node)
        }


        // Processes
        listOf(SbgnType.PROCESS, SbgnType.OMITTED_PROCESS, SbgnType.UNCERTAIN_PROCESS, SbgnType.ASSOCIATION, SbgnType.DISSOCIATION).forEach { type ->
            addPaletteNode{ node, graph ->
                node.type = type
                node.setSbgnProperty(SbgnPropertyKey.ORIENTATION,"horizontal")
                SbgnBuilder.addPorts(graph, node)
                SbgnBuilder.configure(graph, node)
            }
        }

        // Phenotype
        addPaletteNode{ node, graph ->
            node.type = SbgnType.PHENOTYPE
            SbgnBuilder.configure(graph, node)
            SbgnBuilder.addNameLabel(graph, node)
        }

        // Tags
        //listOf("right", "left", "up", "down").forEach { orientation ->
        listOf("right").forEach { orientation ->
            addPaletteNode{ node, graph ->
                node.type = SbgnType.TAG
                node.setSbgnProperty(SbgnPropertyKey.ORIENTATION, orientation)
                SbgnBuilder.configure(graph, node)
                SbgnBuilder.addNameLabel(graph, node)
            }
        }

        // Logical operators
        listOf(SbgnType.AND, SbgnType.OR, SbgnType.NOT).forEach { type ->
            addPaletteNode { node, graph ->
                node.type = type
                node.setSbgnProperty(SbgnPropertyKey.ORIENTATION,"vertical")
                SbgnBuilder.addPorts(graph, node)
                SbgnBuilder.configure(graph, node)
            }
        }

        // Compartment
        addPaletteNode{ node, graph ->
            node.type = SbgnType.COMPARTMENT
            graph.setIsGroupNode(node, true)
            SbgnBuilder.configure(graph, node)
            SbgnBuilder.addNameLabel(graph, node)
        }

        // Submap
        addPaletteNode{ node, graph ->
            node.type = SbgnType.SUBMAP
            graph.setIsGroupNode(node, true)
            SbgnBuilder.configure(graph, node)
            SbgnBuilder.addNameLabel(graph, node)

            val westPort = graph.addPort(node)
            westPort.type = SbgnType.TERMINAL
            westPort.setSbgnProperty(SbgnPropertyKey.TERMINAL_LABEL,"A")
            westPort.setSbgnProperty(SbgnPropertyKey.TERMINAL_SIZE, SizeD(40.0,30.0))
            SbgnBuilder.configure(graph, westPort, PointD(0.0,0.5))

            val eastPort = graph.addPort(node)
            eastPort.type = SbgnType.TERMINAL
            eastPort.setSbgnProperty(SbgnPropertyKey.TERMINAL_LABEL,"B")
            eastPort.setSbgnProperty(SbgnPropertyKey.TERMINAL_SIZE, SizeD(40.0,30.0))
            SbgnBuilder.configure(graph, eastPort, PointD(1.0,0.5))
        }

        // Annotation
        addPaletteNode { node, graph ->
            node.type = SbgnType.ANNOTATION
            SbgnBuilder.configure(graph,node)
            SbgnBuilder.addNameLabel(graph, node)
        }

        addSection() //"Arcs")

        // Connecting Arcs
        listOf(SbgnType.CONSUMPTION, SbgnType.PRODUCTION, SbgnType.MODULATION, SbgnType.STIMULATION, SbgnType.CATALYSIS, SbgnType.INHIBITION, SbgnType.NECESSARY_STIMULATION).forEach { type ->
            addPaletteEdge(PointD(0.0,20.0), PointD(40.0, 0.0)) { edge, graph ->
                graph.addBend(edge, PointD(0.0,0.0))
                edge.type = type
                SbgnBuilder.configure(graph,edge)
            }
        }

        // Equivalence Arc
        addPaletteEdge(PointD(0.0,0.0), PointD(40.0, 0.0)) { edge, graph ->
            //graph.addBend(edge, PointD(0.0,0.0))
            edge.type = SbgnType.EQUIVALENCE_ARC
            SbgnBuilder.configure(graph,edge)
        }

        // Logic Arc
        addPaletteEdge(PointD(0.0,0.0), PointD(0.0, 40.0)) { edge, graph ->
            //graph.addBend(edge, PointD(0.0,0.0))
            edge.type = SbgnType.LOGIC_ARC
            SbgnBuilder.configure(graph,edge)
        }

        addSection() //"Features and Labels")

        // Clone Marker
        addPaletteNodeFeature(0.8){ _, feature, graph ->
            feature.type = SbgnType.CLONE_MARKER
            SbgnBuilder.configure(graph, feature)
            //val cloneLabel = graph.addLabel(node,"marker")
            //cloneLabel.type = SbgnType.CLONE_LABEL
            //SbgnBuilder.configure(graph, cloneLabel)
        }

        // Multimer
        addPaletteNodeFeature(0.6){ node, feature, graph ->
            feature.type = SbgnType.MULTIMER
            graph.setNodeLayout(node, RectD(0.0,0.0,60.0,40.0))
            SbgnBuilder.configure(graph, feature)
            //val cloneLabel = graph.addLabel(node,"marker")
            //cloneLabel.type = SbgnType.CLONE_LABEL
            //SbgnBuilder.configure(graph, cloneLabel)
        }

        // Terminal
        addPalettePort { node, port, graph ->
            modelGraph.setNodeLayout(node, RectD(0.0,0.0,80.0,80.0))
            port.type = SbgnType.TERMINAL
            port.setSbgnProperty(SbgnPropertyKey.TERMINAL_LABEL, "A")
            port.setSbgnProperty(SbgnPropertyKey.TERMINAL_SIZE, SizeD(40.0, 30.0))
            SbgnBuilder.configure(graph, port, PointD(0.0,0.5))
        }

        // Auxiliary Items
        listOf(SbgnType.STATE_VARIABLE, SbgnType.UNIT_OF_INFORMATION).forEach { type ->
            addPaletteNodeLabel{ _, label, graph ->
                label.type = type
                SbgnBuilder.configure(graph, label)
            }
        }

        //Annotation Label
        addPaletteNodeLabel{ _, label, graph ->
            label.type = SbgnType.CALLOUT_LABEL
            label.setSbgnProperty(SbgnPropertyKey.CALLOUT_POINT,PointD(0.3, 0.3))
            graph.setLabelText(label, SbgnBuilder.getDefaultLabelText(label))
            SbgnBuilder.configure(graph, label)
        }

        // Cardinality
        addPaletteEdgeLabel(PointD(0.0,0.0), PointD(40.0, 0.0)) { _, label, graph ->
            label.type = SbgnType.CARDINALITY
            graph.setLabelText(label, "2")
            SbgnBuilder.configure(graph, label)
        }
    }

    fun createStyleTemplateMap(): StyleTemplateMap<SbgnType> {
        val templateMap = mutableMapOf<SbgnType, MutableMap<StyleProperty, Any?>>()
        for(index in 0 until itemCount) {
            val item = getPaletteModelItem(index)
            if(item != null) {
                val styleMap = mutableMapOf<StyleProperty, Any?>()
                when (item) {
                    is INode -> (item.style as IStyleable)
                    is IEdge -> (item.style as IStyleable)
                    is ILabel -> (item.style as IStyleable)
                    is IPort -> (item.style as IStyleable)
                    is IModelItemFeature -> item.style
                    else -> null
                }?.retrieveStyle(item, getItemGraph(index), styleMap)
                if(item is INode) {
                    styleMap[StyleProperty.Width] = item.layout.width
                    styleMap[StyleProperty.Height] = item.layout.height
                }
                if(styleMap.isNotEmpty()) templateMap[item.type] = styleMap
            }
        }
        return templateMap
    }
}
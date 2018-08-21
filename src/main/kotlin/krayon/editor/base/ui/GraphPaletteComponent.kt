/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.InsetsD
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.geometry.SizeD
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.styles.AbstractNodeStyle
import com.yworks.yfiles.graph.styles.PolylineEdgeStyle
import com.yworks.yfiles.graph.styles.ShapeNodeStyle
import com.yworks.yfiles.view.*
import krayon.editor.base.model.IModelItemFeature
import krayon.editor.base.model.SimpleFeature
import krayon.editor.base.util.translate
import krayon.editor.base.util.unaryMinus
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.util.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.ListSelectionListener

open class GraphPaletteComponent(val modelGraph:IGraph = DefaultGraph()) : JPanel() {

    val sections = mutableListOf<Section>()
    var cellRenderer:ListCellRenderer<INode> = PaletteNodeRenderer()
    var isShiftDown = false

    private var currentSection: Section? = null

    init {
        layout = GridBagLayout()
        border = emptyBorder(5)
    }

    var tooltipProvider: ((Int) -> String?)? = null

    private fun getToolTipProviderForSection(section:Any):((Int) -> String?)? {
        if(tooltipProvider == null) return tooltipProvider
        var offset = 0
        for (s in sections) {
            if(s === section) return { tooltipProvider!!.invoke(it+offset)}
            else offset += s.model.size
        }
        return tooltipProvider
    }

    override fun setBackground(bg: Color?) {
        super.setBackground(bg)
        @Suppress("UNNECESSARY_SAFE_CALL")
        sections?.forEach { it.background = bg }
    }

    private fun createGridBagConstraints(gridy:Int, weighty:Double):GridBagConstraints {
        return GridBagConstraints().apply {
            weightx = 1.0
            this.weighty = weighty
            anchor = GridBagConstraints.PAGE_START
            fill = GridBagConstraints.HORIZONTAL
            gridx = 0
            this.gridy = gridy
            ipadx = 0
            ipady = 5
        }
    }

    fun addSection() {
        val newSection = createSection()

        if(componentCount > 0) {  //always have the whole weight on the last section
            (layout as GridBagLayout).setConstraints(sections.last(), createGridBagConstraints(componentCount-1, 0.0))
        }

        if(sections.isNotEmpty()) {
            val line = object:JComponent() {
                override fun paint(g: Graphics) {
                    g.color = Color(230,230,230) //Color.lightGray
                    g.drawLine(0,1,width,1)
                }
            }
            line.isOpaque = true
            line.preferredSize = Dimension(100,2)
            add(line, createGridBagConstraints(componentCount, 0.0))
        }

        add(newSection, createGridBagConstraints(componentCount, 1.0))
        sections += newSection
        currentSection = newSection
    }

    inner class Section:JList<INode>() {
        init {
            addKeyListener(object:KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    isShiftDown = e.isShiftDown
                }

                override fun keyReleased(e: KeyEvent?) {
                    isShiftDown = false
                }
            })
            selectionModel
        }


        private fun computeVisibleColumnCount():Int {
            val cellWidth = (cellRenderer as PaletteNodeRenderer).maxIconWidth
            val width = visibleRect.width
            return width / cellWidth
        }
        fun fixRowCountForVisibleColumns() {
            val cols = computeVisibleColumnCount()
            if(cols > 0) {
                var visRows = model.size / cols
                if (model.size % cols > 0) visRows++
                visibleRowCount = visRows
            }
        }

        override fun getFixedCellWidth(): Int {
            return (cellRenderer as PaletteNodeRenderer).maxIconWidth
        }
        override fun getFixedCellHeight(): Int {
            return (cellRenderer as PaletteNodeRenderer).maxIconHeight
        }

        fun clearSelectionInOtherSections() {
            sections.forEach {
                if(it != this) it.clearSelection()
            }
        }

        inner class CombinedSelectionModel : DefaultListSelectionModel() {
            override fun setSelectionInterval(index0: Int, index1: Int) {
                val sectionIndex = sections.indexOf(this@Section)
                //println("setSelectedInterval $index0 <--> $index1  isShiftDown=$isShiftDown")
                //println("lastSelectionSectionIndex=$lastSelectionSectionIndex  sectionIndex=$sectionIndex lastAnchorIndex=$lastSelectionAnchorIndex")
                if(!isShiftDown) {
                    if (index0 == index1 && this@GraphPaletteComponent.selectedIndices.size <= 1) {
                        if (isSelectedIndex(index0)) {
                            super.removeSelectionInterval(index0, index1)
                            clearSelectionInOtherSections()
                        }
                        else {
                            super.setSelectionInterval(index0, index1)
                            clearSelectionInOtherSections()
                        }
                    } else if(index0 == index1) {
                        super.setSelectionInterval(index0, index1)
                        clearSelectionInOtherSections()
                    }
                } else {
                    var otherSection = sections.elementAt(lastSelectionSectionIndex)
                    when {
                        lastSelectionSectionIndex < sectionIndex -> {
                            (otherSection.selectionModel as CombinedSelectionModel).superSetSelectionInteral(lastSelectionAnchorIndex, otherSection.model.size - 1)
                            while (++lastSelectionSectionIndex != sectionIndex) {
                                otherSection = sections.elementAt(lastSelectionSectionIndex)
                                (otherSection.selectionModel as CombinedSelectionModel).superSetSelectionInteral(0, otherSection.model.size - 1)
                            }
                            super.setSelectionInterval(0, index1)
                        }
                        lastSelectionSectionIndex > sectionIndex -> {
                            (otherSection.selectionModel as CombinedSelectionModel).superSetSelectionInteral(0, lastSelectionAnchorIndex)
                            while (--lastSelectionSectionIndex != sectionIndex) {
                                otherSection = sections.elementAt(lastSelectionSectionIndex)
                                (otherSection.selectionModel as CombinedSelectionModel).superSetSelectionInteral(0, otherSection.model.size - 1)
                            }
                            super.setSelectionInterval(index1, model.size-1)
                        }
                        else -> super.setSelectionInterval(index0, index1)
                    }
                }
                lastSelectionSectionIndex = sectionIndex
                lastSelectionAnchorIndex = anchorSelectionIndex
            }

            override fun setAnchorSelectionIndex(anchorIndex: Int) {
                println("setAnchor $anchorIndex")
                super.setAnchorSelectionIndex(anchorIndex)
            }

            override fun setLeadSelectionIndex(leadIndex: Int) {
                println("setLead $leadIndex")
                super.setLeadSelectionIndex(leadIndex)
            }

            private fun superSetSelectionInteral(index0:Int, index1:Int) {
                super.setSelectionInterval(index0, index1)
            }
        }

    }

    var lastSelectionSectionIndex:Int = 0
    var lastSelectionAnchorIndex:Int = -1

    private fun createSection(): Section {
        return Section().apply {
            addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    //println("componentResized $size")
                    fixRowCountForVisibleColumns()
                }
            })
            layoutOrientation = JList.HORIZONTAL_WRAP
            visibleRowCount = -1

            val defaultModel = DefaultListModel<INode>()
            model = defaultModel

            // set a custom cell renderer painting the sample nodes on the palette
            cellRenderer = this@GraphPaletteComponent.cellRenderer

            // configure the palette as source for drag and drop operations
            dragEnabled = true

            transferHandler = object : TransferHandler("selectedValue") {
                //ensure that the pressed item is chosen, even if mutliselection is active
                override fun exportAsDrag(comp: JComponent?, e: InputEvent?, action: Int) {
                    (e as? MouseEvent)?.let {
                        val index = locationToIndex(it.point)
                        if (index >= 0 && selectedIndices.size > 1) selectedIndex = index
                    }
                    super.exportAsDrag(comp, e, action)
                }
            }
            transferHandler.dragImage = BufferedImage(1, 1,
                    BufferedImage.TYPE_INT_ARGB)
 
            //toggle single selection on click
            selectionModel = CombinedSelectionModel()
        }
    }

    open fun invalidateRenderer() {
        sections.forEach {
            (it.cellRenderer as? PaletteNodeRenderer)?.invalidateCache()
            repaint()
        }
    }

    fun addPaletteNodeLabel(configure: (INode, ILabel, IGraph) -> Unit) {
        val node = modelGraph.createNode()
        val label = modelGraph.addLabel(node, "")
        modelGraph.setStyle(node, ShapeNodeStyle().apply { pen = Pen.getLightGray(); paint = null })
        modelGraph.setNodeLayout(node, RectD(0.0, 0.0, 40.0, 40.0))
        node.tag = label

        configure(node, label, modelGraph)

        (currentSection?.model as DefaultListModel<INode>).addElement(node)
    }

    private fun addElement(node:INode) {
        if(currentSection == null) addSection()
        (currentSection?.model as DefaultListModel<INode>).addElement(node)
    }

    private fun getElementAt(globalIndex: Int): INode {
        var sectionIndex = 0
        var index = globalIndex
        while (index >= sections[sectionIndex].model.size) {
            index -= sections[sectionIndex].model.size
            sectionIndex++
        }
        return sections[sectionIndex].model.getElementAt(index)
    }

    private fun getPaletteNodeLabel(index: Int): ILabel? {

        val pNode = getElementAt(index)
        return pNode.tag as? ILabel
    }

    fun addPaletteNode(configure: (node: INode, graph: IGraph) -> Unit) {
        val node = modelGraph.createNode()
        configure(node, modelGraph)
        addElement(node)
    }


    private fun getPaletteNode(index: Int): INode? {
        val pNode = getElementAt(index)
        return if (pNode.tag is ILabel || pNode.tag is IGraph || pNode.tag is IPort) null else pNode
    }

    fun addPalettePort(configure: (INode, IPort, IGraph) -> Unit) {
        val node = modelGraph.createNode()
        val port = modelGraph.addPort(node)
        modelGraph.setStyle(node, ShapeNodeStyle().apply { pen = Pen.getLightGray(); paint = null })
        modelGraph.setNodeLayout(node, RectD(0.0, 0.0, 40.0, 40.0))
        node.tag = port
        configure(node, port, modelGraph)
        addElement(node)
    }

    private fun getPalettePort(index: Int): IPort? {
        val pNode = getElementAt(index)
        return pNode.tag as? IPort
    }

    fun addPaletteEdge(sourceLocation: PointD, targetLocation: PointD, configure: (IEdge, IGraph) -> Unit) {
        val edgeGraph = DefaultGraph()
        val n1 = edgeGraph.createNode(RectD.fromCenter(sourceLocation, SizeD(1.0, 1.0)), ShapeNodeStyle().apply { paint = null; pen = Pen.getTransparent() })
        val n2 = edgeGraph.createNode(RectD.fromCenter(targetLocation, SizeD(1.0, 1.0)), ShapeNodeStyle().apply { paint = null; pen = Pen.getTransparent() })
        val edge = edgeGraph.createEdge(n1, n2)
        edgeGraph.tag = edge
        configure(edge, edgeGraph)
        addPaletteGraph(edgeGraph)
    }

    private fun getPaletteEdge(index: Int): IEdge? {
        val pNode = getElementAt(index)
        return (pNode.tag as? IGraph)?.tag as? IEdge
    }

    fun getItemGraph(index: Int): IGraph {
        val pNode = getElementAt(index)
        return if (pNode.tag !is IGraph) modelGraph
        else pNode.tag as IGraph
    }

    fun addPaletteEdgeLabel(sourceLocation: PointD, targetLocation: PointD, configure: (IEdge, ILabel, IGraph) -> Unit) {
        val edgeLabelGraph = DefaultGraph()
        val n1 = edgeLabelGraph.createNode(RectD.fromCenter(sourceLocation, SizeD(1.0, 1.0)), ShapeNodeStyle().apply { paint = null; pen = Pen.getTransparent() })
        val n2 = edgeLabelGraph.createNode(RectD.fromCenter(targetLocation, SizeD(1.0, 1.0)), ShapeNodeStyle().apply { paint = null; pen = Pen.getTransparent() })
        val edge = edgeLabelGraph.createEdge(n1, n2)
        edgeLabelGraph.setStyle(edge, PolylineEdgeStyle().apply { pen = Pen.getLightGray() })
        val label = edgeLabelGraph.addLabel(edge, "")
        edgeLabelGraph.tag = label
        configure(edge, label, edgeLabelGraph)
        addPaletteGraph(edgeLabelGraph)
    }

    fun addPaletteNodeFeature(zoom: Double = 1.0, configure: (INode, IModelItemFeature, IGraph) -> Unit) {
        val featureGraph = DefaultGraph()
        val featureNode = featureGraph.createNode()
        val feature = SimpleFeature(featureNode)
        featureGraph.tag = feature
        configure(featureNode, feature, featureGraph)
        addPaletteGraph(featureGraph, zoom)
    }

    private fun getPaletteNodeFeature(index: Int): IModelItemFeature? {
        val pNode = getElementAt(index)
        return (pNode.tag as? IGraph)?.tag as? IModelItemFeature
    }

    private fun getPaletteEdgeLabel(index: Int): ILabel? {
        val pNode = getElementAt(index)
        return (pNode.tag as? IGraph)?.tag as? ILabel
    }

    var graphRendererProvider: (IGraph) -> GraphComponent = { GraphComponent().apply { graph = it } }

    @Suppress("MemberVisibilityCanBePrivate")
    open fun addPaletteGraph(innerGraph: IGraph, zoom: Double = 1.0) {
        val graphComponent = graphRendererProvider.invoke(innerGraph)
        //graphComponent.graph = innerGraph
        graphComponent.setBounds(0, 0, 400, 400)
        graphComponent.updateContentRect()
        val contentRect = graphComponent.contentRect
        innerGraph.translate(-contentRect.toPointD())
        val nodeBox = RectD(contentRect.x, contentRect.y, contentRect.width * zoom, contentRect.height * zoom)
        //val node = modelGraph.createNode(contentRect, GraphNodeStyle(),innerGraph)

        val node = modelGraph.createNode(nodeBox, GraphNodeStyle(zoom, graphRendererProvider), innerGraph)
        node.tag = innerGraph
        addElement(node)
    }

    fun getPaletteGraph(index: Int): IGraph? {
        val pNode = getElementAt(index)
        val pGraph = pNode.tag as? IGraph
        return if (pGraph != null && pGraph.tag !is ILabel && pGraph.tag !is IEdge && pGraph.tag !is IModelItemFeature) pGraph else null
    }

    fun getPaletteModelItem(index: Int): IModelItem? {
        return getPaletteNode(index) ?: getPaletteEdge(index) ?: getPaletteNodeLabel(index)
        ?: getPaletteEdgeLabel(index) ?: getPalettePort(index) ?: getPaletteNodeFeature(index)
    }

    fun clearSelection() {
        for (section in sections) {
            section.clearSelection()
        }
    }

    fun addListSelectionListener(listener: ListSelectionListener) {
        sections.forEach { it.addListSelectionListener(listener) }
    }

    val selectedIndices: IntArray get() {
        val list = mutableListOf<Int>()
        var offset = 0
        for (section in sections) {
            for (selectedIndex in section.selectedIndices) {
                list.add(selectedIndex + offset)
            }
            offset += section.model.size
        }
        return list.toIntArray()
    }
    var selectionMode: Int
        get() = sections[0].selectionMode
        set(value) {
            sections.forEach{ it.selectionMode = value}
        }

    var selectionBackground: Color
        get() = sections[0].selectionBackground
        set(value) { sections.forEach { it.selectionBackground = value }}


    val itemCount:Int get() {
        var count = 0
        for (section in sections) { count += section.model.size }
        return count
    }

    open class GraphNodeStyle(private val zoom:Double = 1.0, private val rendererProvider:((IGraph)->GraphComponent)? = null) : AbstractNodeStyle() {

        override fun createVisual(context: IRenderContext, node: INode): IVisual {
            // create a GraphComponent instance and add a copy of the given node with its labels
            val innerGraph = node.tag as IGraph
            val graphComponent = rendererProvider?.invoke(innerGraph) ?: GraphComponent().apply { graph = innerGraph }

            graphComponent.updateContentRect()
            val contentRect = graphComponent.contentRect
            val configurator = ContextConfigurator(contentRect)
            val renderContext = configurator.createRenderContext(graphComponent)

            return IVisual { ctx, g -> g.create { gfx ->
                gfx.translate(node.layout.x,node.layout.y)
                gfx.scale(zoom,zoom)
                graphComponent.exportContent(renderContext).paint(ctx,gfx)
            }}
        }
    }

    class PaletteNodeRenderer : ListCellRenderer<INode> {

        private val renderer: DefaultListCellRenderer = DefaultListCellRenderer()
        private val node2icon: WeakHashMap<INode, NodeIcon> = WeakHashMap()

        var maxIconWidth = 100
        var maxIconHeight = 100
        var iconInsets = InsetsD(5.0)
        @Suppress("MemberVisibilityCanBePrivate")
        var iconBorder: Border = emptyBorder(0)

        fun invalidateCache() {
            node2icon.clear()
        }

        override fun getListCellRendererComponent(list: JList<out INode>, node: INode, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
            // we use a label as component that renders the list cell and sets the icon that paints the given node
            val label = renderer.getListCellRendererComponent(list, node, index, isSelected, cellHasFocus) as JLabel
            label.border = iconBorder
            label.horizontalAlignment = SwingConstants.CENTER
            label.icon = getIcon(node)
            label.iconTextGap = 0
            label.text = null
            label.toolTipText = (list.parent as GraphPaletteComponent).getToolTipProviderForSection(list)?.invoke(index)
            return label
        }

        /**
         * Returns an [Icon] painting the given node.
         */
        private fun getIcon(node: INode): Icon {
            var icon: NodeIcon? = node2icon[node]
            if (icon == null) {
                icon = NodeIcon(node)
                node2icon[node] = icon
            }
            return icon
        }

        /**
         * An [Icon] that paints an [INode].
         */
        private inner class NodeIcon internal constructor(node: INode) : Icon {
            internal val image: BufferedImage

            init {
                // create a GraphComponent instance and add a copy of the given node with its labels

                val graphComponent = GraphComponent()

                val newLayout = RectD(PointD.ORIGIN, node.layout.toSizeD())
                val newNode = graphComponent.graph.createNode(newLayout, node.style, node.tag)
                node.labels.forEach { label -> graphComponent.graph.addLabel(newNode, label.text, label.layoutParameter, label.style, label.preferredSize, label.tag) }
                node.ports.forEach { port -> graphComponent.graph.addPort(newNode, port.locationParameter, port.style).also { it.tag = port.tag } }
                // create an image of the node with its labels

                graphComponent.updateContentRect()
                val pixelImageExporter = PixelImageExporter(graphComponent.contentRect.getEnlarged(iconInsets))

                pixelImageExporter.isTransparencyEnabled = true
                val scale1 = Math.min(1.0, pixelImageExporter.configuration.calculateScaleForWidth(maxIconWidth.toDouble()))
                val scale2 = Math.min(1.0, pixelImageExporter.configuration.calculateScaleForHeight(maxIconHeight.toDouble()))
                pixelImageExporter.configuration.scale = Math.min(scale1, scale2)
                image = pixelImageExporter.exportToBitmap(graphComponent)
            }

            override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
                g.drawImage(image, x, y, null)
            }

            override fun getIconWidth(): Int {
                return image.width
            }

            override fun getIconHeight(): Int {
                return image.height
            }

        }

    }
}
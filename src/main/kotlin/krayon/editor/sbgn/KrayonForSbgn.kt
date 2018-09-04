/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn

import com.yworks.yfiles.graph.*
import com.yworks.yfiles.view.input.PopulateItemPopupMenuEventArgs
import krayon.editor.base.Application
import krayon.editor.base.command.*
import krayon.editor.base.command.PrintPreview
import krayon.editor.base.style.GraphStyle
import krayon.editor.base.style.StyleManager
import krayon.editor.base.style.StyleProperty
import krayon.editor.base.ui.*
import krayon.editor.base.util.ApplicationSettings
import krayon.editor.base.util.IconManager
import krayon.editor.base.util.ceim
import krayon.editor.sbgn.command.*
import krayon.editor.sbgn.io.SbgnReader
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.graphStyle
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.SbgnBuilder
import krayon.editor.sbgn.style.SbgnBuilder.styleManager
import krayon.editor.sbgn.ui.*
import krayon.util.OperatingSystemChecker
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.EventQueue
import java.beans.PropertyChangeListener
import java.io.File
import java.io.InputStreamReader
import javax.swing.*

object KrayonForSbgn {

    private const val appName = "krayon4sbgn"
    private const val appTitle = "Krayon for SBGN"
    private const val systemStylePath = "/resources/styles/read-only"
    private val appHome = getAppHome()
    val userStylePath = "$appHome/styles"
    private val settingsPath = "$appHome/application-settings.xml"
    private const val systemBricksPath = "/resources/bricks/pd"
    private const val stringMapPath = "/resources/actions/en/strings.json"
    private const val keyMapPath = "/resources/actions/keys.json"
    private const val iconMapPath = "/resources/icons/icons.json"

    private lateinit var palette: ConfiguredSbgnPaletteComponent
    private lateinit var bricksPalette: SbgnPaletteComponent
    private lateinit var editorContainer: JPanel
    private lateinit var propertyTable: PropertyTable
    private lateinit var paletteContainer: JScrollPane
    private lateinit var propertyTableContainer:JScrollPane
    private lateinit var tableAndBrickPane:JSplitPane
    private var tableAndBrickPaneDividerSize:Int = 0
    private var propertyTablePreferredHeight:Int = 200

    val graphComponent get() = Application.focusedGraphComponent as SbgnGraphComponent

    fun start() {
        ApplicationSettings.apply {
            backingFile = File(settingsPath)
            load()
            ApplicationSettings.APPLICATION_TITLE.value = appTitle
            ApplicationSettings.APPLICATION_RESOURCE_PATH.value = "/resources"
            ApplicationSettings.APPLICATION_ICON.value = "/resources/icons/krayon.png"
            ApplicationSettings.CANVAS_ICON_PATH.value = "/resources/icons/canvas"
            ApplicationSettings.DEFAULT_HIGHLIGHT_COLOR.value = Color(161,192,87)
            IconManager.iconMapPath = iconMapPath
        }
        SetLookAndFeel.execute(SetLookAndFeel.LnF.MATERIAL)

        ToolTipManager.sharedInstance().apply {
            initialDelay = 2000
            reshowDelay = 2000
        }

        initializeActions()

        val frame = createFrame()
        configure(frame.rootPane)

//        frame.preferredSize = Dimension(1700,956)
//        frame.pack()
        frame.isVisible = true

        SwingUtilities.invokeLater {
            paletteContainer.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        }
    }

    private fun initializeActions() {
        CommandManager += ActivateSbgnStrictMode
        CommandManager += AddStateVariable
        CommandManager += AddUnitOfInformation
        CommandManager += AutoAssignCloneMarkers
        CommandManager += ConvertToComplex
        CommandManager += CreateNode
        CommandManager += CyclePermittedNodes
        CommandManager += Delete
        CommandManager += DeselectAll
        CommandManager += DrawingOrderCommands.NodesToBack
        CommandManager += DrawingOrderCommands.NodesToFront
        CommandManager += DumpTypeInfo
        CommandManager += EditLabel
        CommandManager += GraphicsExportPreview
        CommandManager += InteractiveDuplicate
        CommandManager += InteractivePaste
        CommandManager += MirrorHorizontally
        CommandManager += MirrorVertically
        CommandManager += OpenSbgn
        CommandManager += PrintPreview
        CommandManager += RotateClockwise
        CommandManager += RotateCounterClockwise
        CommandManager += SavePlainSbgn
        CommandManager += SaveSbgn
        CommandManager += SelectAll
        CommandManager += ShowHelp
        CommandManager += ShowSettings
        CommandManager += SplitNode
        CommandManager += ToggleComplexLock
        CommandManager += ToggleEventMonitor
        CommandManager += ToggleCloneMarker
        CommandManager += ToggleFullScreenMode
        CommandManager += SetLookAndFeel
        CommandManager += ToggleMultimer
        CommandManager += YFilesCommands.Copy
        CommandManager += YFilesCommands.Cut
        CommandManager += YFilesCommands.FitGraphBounds
        CommandManager += YFilesCommands.Redo
        CommandManager += YFilesCommands.Undo
        CommandManager += YFilesCommands.ZoomIn
        CommandManager += YFilesCommands.ZoomOut
        CommandManager += YFilesCommands.ZoomToNormal
        CommandManager += YFilesCommands.SelectAbove
        CommandManager += YFilesCommands.SelectBelow
        CommandManager += YFilesCommands.SelectRight
        CommandManager += YFilesCommands.SelectLeft
        CommandManager += ConstrainedCreateEdgeInputMode.BeginEdgeCreation
        CommandManager += ConstrainedCreateEdgeInputMode.EndEdgeCreation
        CommandManager += ConstrainedCreateEdgeInputMode.NextType
        CommandManager += SbgnPaletteDropInputMode.RotateClockwise
        CommandManager += SbgnPaletteDropInputMode.MirrorHorizontally
        CommandManager += SbgnPaletteDropInputMode.MirrorVertically

        CommandManager += StyleManagementToolBar.ApplyStyleToDiagram
        CommandManager += StyleManagementToolBar.ApplyStyleToSelection
        CommandManager += StyleManagementToolBar.NextStyleInUse

        CommandManager += UnicodeTextEditorInputMode.ConvertToGreek
        CommandManager += UnicodeTextEditorInputMode.ConvertToSubscript
        CommandManager += UnicodeTextEditorInputMode.ConvertToSuperscript


        javaClass.getResourceAsStream(keyMapPath).use {
            CommandManager.initializeKeyMap(it)
        }
        javaClass.getResourceAsStream(stringMapPath).use {
            CommandManager.initializeTextualResources(it)
        }


    }

    private fun addNewGraphComponent() {
        val newGraphComponent = SbgnGraphComponent()
        configureGraphComponent(newGraphComponent)
        editorContainer.add(newGraphComponent)
        Application.focusedGraphComponent = newGraphComponent
    }

    private fun configureGraphComponent(graphComponent: SbgnGraphComponent) {
        val geim = graphComponent.createEditorMode()
        geim.nodeDropInputMode = GraphPaletteDropInputMode(palette.modelGraph)
        geim.nodeDropInputMode = SbgnPaletteDropInputMode(palette.modelGraph)
        geim.popupMenuItems = GraphItemTypes.NODE
        geim.addPopulateItemPopupMenuListener(::onPopulateItemPopupMenu)
        graphComponent.inputMode = geim
        CommandManager.registerKeyBindings(graphComponent.ceim.keyboardInputMode)
    }

    private fun createPaletteComponent():ConfiguredSbgnPaletteComponent {
        return ConfiguredSbgnPaletteComponent().apply {
            selectionMode = javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
            tooltipProvider = { index ->
                getPaletteModelItem(index)?.let{ item ->
                    item.type.name.split('_').joinToString(" ") { it[0] + it.substring(1).toLowerCase() }
                }
            }
            (cellRenderer as GraphPaletteComponent.PaletteNodeRenderer).also {
                it.maxIconHeight = 40
                it.maxIconWidth = 65
            }
        }
    }

    private fun createBricksComponent(modelGraph:IGraph):SbgnPaletteComponent {
        return BricksPaletteComponent(modelGraph).apply {
            (cellRenderer as GraphPaletteComponent.PaletteNodeRenderer).also {
                it.maxIconHeight = 80
                it.maxIconWidth = 140
            }
            InputStreamReader(javaClass.getResourceAsStream("$systemBricksPath/bricks.list")).forEachLine { line ->
                if(!line.startsWith("#")) {
                    if (line.startsWith("--")) addSection()
                    else {
                        val (id, fileName, tooltip) = line.split(',').map { it.trim() }
                        val brickGraph = DefaultGraph()
                        brickGraph.tag = tooltip
                        tooltipProvider = { (getPaletteGraph(it) as IGraph).tag as String }
                        javaClass.getResourceAsStream("$systemBricksPath/$fileName").use {
                            SbgnReader().read(it, brickGraph, null)
                            addBrick(brickGraph, id)
                        }
                    }
                }
            }
        }
    }

    private fun configure(rootPane: JRootPane) {
        val contentPane = rootPane.contentPane
        palette = createPaletteComponent()

        editorContainer = JPanel(BorderLayout())
        addNewGraphComponent()

        val sidePaneWidth = 360
        paletteContainer = JScrollPane(palette)
        paletteContainer.preferredSize = Dimension(sidePaneWidth, 460)
        paletteContainer.verticalScrollBar.unitIncrement = 20
        paletteContainer.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER


        bricksPalette = createBricksComponent(palette.modelGraph)
        val bricksContainer = JScrollPane(bricksPalette)
        bricksContainer.preferredSize = Dimension(sidePaneWidth, 440)
        bricksContainer.verticalScrollBar.unitIncrement = 20

        val defaultStyle = createDefaultStyle(palette)


        SbgnBuilder.styleManager.apply {
            addStyle(defaultStyle)
            InputStreamReader(javaClass.getResourceAsStream("$systemStylePath/styles.list")).forEachLine {
                try {
                    javaClass.getResourceAsStream("$systemStylePath/$it.css").use { stream ->
                        addStyleFromStream(stream, it, readOnly = true)
                    }
                }catch (ex:Exception) {
                    ProblemReporter.reportThrowable(ex, "Problem parsing $it.css", graphComponent)
                }
            }
            addStylesFromDir(userStylePath, readOnly = false)
            currentStyle = styles.find { it.name == ApplicationSettings.DEFAULT_SBGN_STYLE.value } ?: defaultStyle
            styleListeners += object:StyleManager.StyleListener<SbgnType> {
                override fun onStyleEvent(graphStyle: GraphStyle<SbgnType>, op: StyleManager.StyleOp) {
                    if(op == StyleManager.StyleOp.CURRENT_STYLE_CHANGED) {
                        updatePaletteStyle(palette, graphStyle)
                        updatePaletteStyle(bricksPalette, graphStyle)
                    }
                    if(op == StyleManager.StyleOp.MODIFY && graphStyle == currentStyle) {
                        updatePaletteStyle(palette, graphStyle)
                        updatePaletteStyle(bricksPalette, graphStyle)
                    }
                    if(op == StyleManager.StyleOp.MODIFY || op == StyleManager.StyleOp.CREATE) {
                        writeStyleToDir(graphStyle, userStylePath)
                    }
                    else if(op == StyleManager.StyleOp.DELETE) {
                        deleteStyleFromDir(graphStyle, userStylePath)
                    }
                    else if(op == StyleManager.StyleOp.SHOW_EDITOR) {
                        tableAndBrickPane.dividerSize = tableAndBrickPaneDividerSize
                        tableAndBrickPane.dividerLocation = 1
                        tableAndBrickPane.setDividerLocationAnimated(propertyTablePreferredHeight)
                    }
                    else if(op == StyleManager.StyleOp.HIDE_EDITOR) {
                        propertyTablePreferredHeight = propertyTableContainer.size.height
                        tableAndBrickPane.dividerSize = 0
                        tableAndBrickPane.setDividerLocationAnimated(1)
                    }
                }
            }

        }

        propertyTable = PropertyTable()
        propertyTableContainer = JScrollPane(propertyTable).apply {
            preferredSize = Dimension(Math.max(palette.width, 200), 0)
            minimumSize = preferredSize
            toolTipText = "Select one or more palette items to change individual style properties"
        }
        tableAndBrickPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, propertyTableContainer, bricksContainer)
        tableAndBrickPaneDividerSize = tableAndBrickPane.dividerSize
        tableAndBrickPane.dividerSize = 0
        tableAndBrickPane.dividerLocation = 0
        tableAndBrickPane.resizeWeight = 0.0

        val styleToolbar = StyleManagementToolBar(styleManager, palette, propertyTable)
        val styleAndPalettePane = JPanel(BorderLayout())
        styleAndPalettePane.add(styleToolbar, BorderLayout.NORTH)
        styleAndPalettePane.add(paletteContainer, BorderLayout.CENTER)

        val sidePane = JSplitPane(JSplitPane.VERTICAL_SPLIT, styleAndPalettePane, tableAndBrickPane)
        sidePane.dividerLocation = paletteContainer.preferredSize.height

        val mainSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorContainer, sidePane)
        mainSplit.resizeWeight = 1.0

        contentPane.add(mainSplit, BorderLayout.CENTER)
        contentPane.add(createToolBar(), BorderLayout.NORTH)

        styleManager.styles.find{ ApplicationSettings.DEFAULT_SBGN_STYLE.value == it.name }?.let {
            graphComponent.applyStyle(it)
        }

        updatePaletteStyle(palette, styleManager.currentStyle!!)
        updatePaletteStyle(bricksPalette, styleManager.currentStyle!!)
    }

    private fun updatePaletteStyle(palette:GraphPaletteComponent, style:GraphStyle<SbgnType>) {
        for (index in 0 until palette.itemCount) {
            palette.getPaletteModelItem(index)?.let {
                styleManager.applyStyle(style, palette.getItemGraph(index), it, applySize = true)
            }
            palette.getPaletteGraph(index)?.let { paletteGraph ->
                styleManager.applyStyle(style, paletteGraph, applySize = true)
            }
        }
        style.styleTemplateMap[SbgnType.MAP]?.forEach { key, value ->
            if(key == StyleProperty.BackgroundColor) {
                palette.background = value as Color
            }
            else if(key == StyleProperty.HighlightColor) {
                palette.selectionBackground =  value as Color
            }
        }
        graphComponent.repaint()
        palette.invalidateRenderer()
    }

    private fun createDefaultStyle(palette: ConfiguredSbgnPaletteComponent): GraphStyle<SbgnType> {
        val defaultStyleMap = palette.createStyleTemplateMap()
        defaultStyleMap[SbgnType.MAP] = graphComponent.createStyleMap()
        return GraphStyle("Canonical", true, defaultStyleMap)
    }

    private fun createFrame(): JFrame {
        val frame = JFrame(ApplicationSettings.APPLICATION_TITLE.value as String)
        frame.iconImage = ImageIcon(ApplicationSettings.APPLICATION_ICON.asResource()).image
        frame.setSize(1365, 868)
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        ApplicationSettings.addPropertyChangeListener(PropertyChangeListener {
            if(ApplicationSettings.DIAGRAM_FILE.name == it.propertyName) {
                val name = if(it.newValue is File) " [${(it.newValue as File).name}]" else ""
                frame.title = "${ApplicationSettings.APPLICATION_TITLE.value}$name"
            }
        })
        return frame
    }

    val ApplicationCommand.action
        get() = getAction(this@KrayonForSbgn.graphComponent)


    private fun createToolBar():JToolBar {
        return JToolBar().apply {
            add(OpenSbgn.action)
            add(SaveSbgn.action)
            add(SavePlainSbgn.action)
            add(PrintPreview.action)
            add(GraphicsExportPreview.action)
            addSeparator()
            add(YFilesCommands.ZoomIn.action)
            add(YFilesCommands.ZoomOut.action)
            add(YFilesCommands.ZoomToNormal.getAction(graphComponent,1))
            add(YFilesCommands.FitGraphBounds.action)
            addSeparator()
            add(YFilesCommands.Undo.action)
            add(YFilesCommands.Redo.action)
            addSeparator()
            add(UiFactory.createStateButton(ActivateSbgnStrictMode.action.apply {
                isSelected = true
                name = null
            }))

            add(Box.createHorizontalGlue())
            add(ShowSettings.action)
            add(ShowHelp.action)
        }
    }

    @Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
    fun onPopulateItemPopupMenu(source:Any, args: PopulateItemPopupMenuEventArgs<IModelItem>)
    {
        val selection = graphComponent.selection
        //val commands = graphComponent.commands

        if(args.isHandled) return

        val menu = args.menu as JPopupMenu
        val node = args.item as? INode

        if(node != null && !graphComponent.selection.isSelected(node)) {
            graphComponent.selection.clear()
            graphComponent.selection.setSelected(node, true)
        }

        if(selection.selectedNodes.any() || selection.selectedEdges.any() || selection.none()) {
            menu.add(createStyleMenu())
        }

        //can act on multiple nodes
        if(selection.selectedNodes.any()) {
            menu.add(EditLabel.action)
            menu.add(CyclePermittedNodes.action)
            menu.add(AddUnitOfInformation.action)
            menu.add(AddStateVariable.action)
            menu.add(ToggleCloneMarker.action)
            menu.add(ToggleMultimer.action)
            menu.add(ToggleComplexLock.action)
            menu.add(ConvertToComplex.action)
            menu.add(SplitNode.action)
            menu.addSeparator()
            menu.add(MirrorHorizontally.action)
            menu.add(MirrorVertically.action)
            menu.add(RotateClockwise.action)
            menu.add(RotateCounterClockwise.action)
            menu.addSeparator()
            menu.add(DrawingOrderCommands.NodesToFront.action)
            menu.add(DrawingOrderCommands.NodesToBack.action)
            menu.addSeparator()
        }

        menu.add(AutoAssignCloneMarkers.action)
        menu.add(YFilesCommands.Cut.action)
        menu.add(YFilesCommands.Copy.action)
        menu.add(InteractivePaste.getAction(graphComponent,false))
        menu.add(InteractiveDuplicate.getAction(graphComponent,false))

        args.isShowingMenuRequested = true
        args.isHandled = true
    }

    private fun setIndividualStyleOnSelection(graphStyle:GraphStyle<SbgnType>?) {
        with(graphComponent) {
            val items = selection.selectedNodes + selection.selectedEdges
            if(items.isEmpty()) {
                applyStyle(graphStyle)
            }
            else items.forEach { item ->
                item.graphStyle = graphStyle
                SbgnBuilder.applyStyle(graph, item)
            }
            repaint()
        }
    }

    private fun createStyleMenu():JMenu {
        val menu = JMenu("Style")
        val styleGroups = (graphComponent.selection.selectedNodes + graphComponent.selection.selectedEdges).groupBy {
            it.graphStyle }
        val commonStyle = styleGroups.keys.firstOrNull()
        val hasCommonStyle = styleGroups.size == 1
        if(!hasCommonStyle && !graphComponent.selection.none()) {
            menu.add(JRadioButtonMenuItem("Undefined").apply { isSelected = true })
            menu.addSeparator()
        }
        menu.add(JRadioButtonMenuItem("Dynamic").apply {
            isSelected = hasCommonStyle && commonStyle == null || (graphComponent.selection.none() && graphComponent.graphStyle == null)
            addActionListener { setIndividualStyleOnSelection(null) }
        })
        menu.addSeparator()
        SbgnBuilder.styleManager.getStylesInDisplayOrder().forEach { style ->
            menu.add(JRadioButtonMenuItem(if(style.isFileLocal) "File:${style.name}" else style.name).apply {
                isSelected = hasCommonStyle && commonStyle == style || (graphComponent.selection.none() && graphComponent.graphStyle == style)
                addActionListener { setIndividualStyleOnSelection(style) }
            })
        }
        return menu
    }

    private fun getAppHome():String {
        return when {
            OperatingSystemChecker.isWindows -> "${System.getenv("APPDATA")}/$appTitle"
            OperatingSystemChecker.isMac -> "${System.getProperty("user.home")}/Library/Application Support/$appTitle"
            else -> "${System.getProperty("user.home")}/.$appName"
        }
    }

    @JvmStatic fun main(args: Array<String>) {
        EventQueue.invokeLater {
            ProblemReporter.installForUncaughtSwingExceptions()
            KrayonForSbgn.start()
        }
    }
}


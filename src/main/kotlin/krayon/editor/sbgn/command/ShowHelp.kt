/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import com.yworks.yfiles.view.input.ICommand
import krayon.editor.base.Application
import krayon.editor.base.command.ApplicationCommand
import krayon.editor.base.command.CommandManager
import krayon.editor.base.command.CommandScope
import krayon.editor.base.ui.JHintingTextField
import krayon.editor.base.ui.openUrlInBrowser
import krayon.editor.base.ui.toShortcutString
import krayon.editor.base.util.ApplicationSettings
import krayon.editor.sbgn.io.SbgnReader
import krayon.editor.sbgn.ui.SbgnGraphComponent
import krayon.util.OperatingSystemChecker
import krayon.util.ResourceLoader
import krayon.util.UnicodeChars
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.InputStreamReader
import java.io.StringReader
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

object ShowHelp : ApplicationCommand("SHOW_HELP") {

    val dialog:JDialog by lazy { createDialog() }

    private fun createDialog():JDialog {
        return JDialog(SwingUtilities.getWindowAncestor(graphComponent),name, Dialog.ModalityType.APPLICATION_MODAL).apply {
            contentPane = JTabbedPane().apply {
                addTab("Actions", createFindActionComponent())
                addTab("Resources", createResourcesComponent())
                addTab("About", createAboutComponent())
            }
            defaultCloseOperation = JDialog.HIDE_ON_CLOSE
            pack()
        }
    }

    override fun canExecute(param: Any?): Boolean {
        return true
    }

    override fun execute(param: Any?) {
        dialog.isVisible = true
    }

    private fun createAboutComponent():JComponent {
        val aboutPane = JTextPane().apply {
            contentType = "text/html"
            isEditable = false
            editorKit = HTMLEditorKit().apply {
                val cssRules = """
                    * { font-family:sans-serif; }
                    .category { font-weight: bold; }
                    h1 { font-weight: bold; font-size:120%; }
                    p { padding: 5px; }

                """.trimIndent()
                styleSheet = StyleSheet().apply {
                    loadRules(StringReader(cssRules), null)
                }
            }
            text = """
                <h1>Krayon for SBGN ${ApplicationSettings.APPLICATION_VERSION.value}</h1>
                <h1>License</h1>
                <p>Copyright 2018 Roland Wiese</p>
                <p>This software with the exception of the 3rd party components listed below is licensed under the Apache License, Version 2.0 (the "License");
                you may not use this software except in compliance with the License.
                You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0</p>
                <p>Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
                WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
                See the License for the specific language governing permissions and limitations under the License.</p>

                <h1>3rd party components used by this software</h1>
                <p>yFiles for Java by yWorks. Commercial License: https://www.yworks.com/products/yfiles-for-java/sla</p>
                <p>LibSBGN, Apache License, Version 2.0</p>
                <p>JH Labs Java Image Filters, Apache License, Version 2.0</p>
                <p>Klaxon JSON Parser, Apache License, Version 2.0</p>
                <p>Kotlin Stdlib, Apache License, Version 2.0</p>
                <p>SBGN Bricks, MIT License</p>
                <p>ph-css, Apache License, Version 2.0</p>
                <p>IconExperience O-Collection Icons, License: https://www.iconexperience.com/o_collection/license</p>
            """.trimIndent()
        }

        return JScrollPane(aboutPane).apply {
            preferredSize = Dimension(400,300)
        }

    }

    private fun createFindActionComponent():JComponent {
        val commands = CommandManager.getCommands()
        val component = JPanel(BorderLayout())
        var activeScopeFilter: CommandScope? = null
        val tableModel = object:DefaultTableModel() {
            init {
                addColumn("Name")
                addColumn("Shortcut")
                commands.forEach { addRow(arrayOf(it, it.keyStroke?.toShortcutString() ?: "")) }
            }
            override fun getColumnClass(columnIndex: Int): Class<*> {
                return when(columnIndex) {
                    0 -> ApplicationCommand::class.java
                    else -> String::class.java
                }
            }
        }

        val table = JTable(tableModel).apply {
            tableHeader.reorderingAllowed = false
            setDefaultRenderer(ApplicationCommand::class.java, object:DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                    return super.getTableCellRendererComponent(table, (value as ApplicationCommand).name, isSelected, hasFocus, row, column)
                }
            })
            autoCreateRowSorter = true
            if(OperatingSystemChecker.isMac && (!font.canDisplay(UnicodeChars.MacAltKey) || !font.canDisplay(UnicodeChars.MacCmdKey))) {
                //switch to font that supports mac cmd/alt key
                font = Font("Dialog", Font.PLAIN, font.size)
            }
        }
        val tableContainer = JScrollPane(table).apply {
            preferredSize = Dimension(400, 300)
        }

        val searchField = JHintingTextField().apply {
            putClientProperty("emptyTextHint", "Search...")
            addActionListener { evt ->
                val text = (evt.source as JTextField).text
                val rowFilter = object:RowFilter<Any, Any>() {
                    override fun include(entry: Entry<out Any, out Any>): Boolean {
                        val command = entry.getValue(0) as ApplicationCommand
                        if(activeScopeFilter != null && activeScopeFilter != command.scope) return false
                        if(command.name?.toLowerCase()?.contains(text.toLowerCase()) == true) return true
                        //if(command.description?.toLowerCase()?.contains(text) == true) return true
                        //if(command.mouseGestureDescription?.toLowerCase()?.contains(text) == true) return true
                        return false
                    }
                }
                (table.rowSorter as TableRowSorter<*>).rowFilter = rowFilter
            }
        }

        val scopeChooser = JPanel().apply {
            add(JLabel("Scope:"))
            add(Box.createHorizontalGlue())
            val buttonGroup = ButtonGroup()
            var button = JRadioButton("All")
            button.addActionListener { activeScopeFilter = null; searchField.postActionEvent() }
            buttonGroup.add(button)
            button.isSelected = true
            add(button)
            CommandScope.values().forEach { scope ->
                button = JRadioButton(scope.getDisplayName())
                button.addActionListener { activeScopeFilter = scope; searchField.postActionEvent()}
                button.toolTipText = getScopeDescription(scope)
                buttonGroup.add(button)
                add(button)
            }
        }

        val detailPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
        }

        val detailPaneContainer = JScrollPane(detailPane).apply {
            preferredSize = Dimension(400,200)
        }

        detailPane.editorKit = HTMLEditorKit().apply {
            val cssRules = """
                * { font-family:sans-serif; }
                div { font-size:"12pt"; }
                .category { font-weight: bold; }
                p { padding: 5px; }
            """.trimIndent()
            styleSheet = StyleSheet().apply {
                loadRules(StringReader(cssRules), null)
            }
        }

        table.selectionModel.addListSelectionListener {
            if(table.selectedRow < 0) detailPane.text = null
            else {
                val command = table.getValueAt(table.selectedRow,0) as ApplicationCommand
                detailPane.text = """<html><div>
                <p><span class="category">Description:</span> ${command.description}</p>
                <p><span class="category">Mouse Gesture:</span> ${command.mouseGestureDescription ?: ""}</p>
                <p><span class="category">Keyboard Shortcut:</span> ${command.keyStroke?.toShortcutString() ?: ""}</p>
                <p><span class="category">Applicable Context:</span> ${getScopeDescription(command.scope)}</p>
                </div></html>"""
            }
        }

        val filterPanel = Box(BoxLayout.Y_AXIS).apply {
            add(searchField)
            add(scopeChooser)
        }

        component.add(filterPanel, BorderLayout.NORTH)
        component.add(tableContainer, BorderLayout.CENTER)
        component.add(detailPaneContainer, BorderLayout.SOUTH)
        return component
    }

    private fun CommandScope.getDisplayName():String {
        return when(this) {
            CommandScope.DEFAULT -> "Canvas has Focus"
            CommandScope.CREATE_EDGE -> "Create Edge"
            CommandScope.DRAG_ITEM -> "Drag Item"
            CommandScope.ENTER_TEXT -> "Enter Text"
        }
    }

    private fun getScopeDescription(scope: CommandScope):String {
        return when(scope) {
            CommandScope.DEFAULT -> "Diagram canvas has focus."
            CommandScope.CREATE_EDGE -> "Interactive edge creation is in progress."
            CommandScope.DRAG_ITEM -> "Interactive drag from palette is in progress."
            CommandScope.ENTER_TEXT -> "Inlined diagram text editing is in progress."
        }
    }

    class ResourceData(val name:String, val path:String?)

    private fun createResourcesComponent():JComponent {
        val model = DefaultListModel<ResourceData>()
        val resourceList = JList<ResourceData>(model)
        val helpPath = "${ApplicationSettings.APPLICATION_RESOURCE_PATH.value}/help"
        val diagramPath = "$helpPath/diagrams"

        resourceList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(evt: MouseEvent) {
                if (evt.clickCount == 2) {
                    val index = resourceList.locationToIndex(evt.point)
                    if (index >= 0) {
                        resourceList.model.getElementAt(index)?.path?.let {
                            if(it.startsWith("http")) {
                                openUrlInBrowser(it)
                            }
                            else displayDiagram("$diagramPath/$it")
                        }
                    }
                }
            }
        })

        resourceList.cellRenderer = object:DefaultListCellRenderer() {
            val bg = background
            val fg = foreground
            override fun getListCellRendererComponent(list: JList<*>?, value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val item = (value as ResourceData).name
                background = bg
                background = fg
                val comp =  super.getListCellRendererComponent(list, item, index, isSelected, cellHasFocus)
                when {
                    value.path == null -> {
                        background = fg
                        foreground = bg
                        toolTipText = null
                    }
                    value.path.startsWith("http") -> toolTipText = "Double click to open video in browser"
                    else -> toolTipText = "Double click to open diagram in editor"
                }
                return comp
            }
        }

        InputStreamReader(ResourceLoader.getResourceAsStream("$helpPath/resources.list")).forEachLine { line ->
            if (!line.startsWith("#")) {
                if (line.startsWith("--")) {
                    model.addElement(ResourceData(line.substring(2), null))
                } else {
                    val entry = line.split(';')
                    val name = if (entry.size <= 1) entry[0] else entry[1]
                    val path = entry[0]
                    model.addElement(ResourceData(name, path))
                }
            }
        }

        return JScrollPane(resourceList).apply {
            preferredSize = Dimension(400,200)
        }

    }

    fun displayDiagram(sampleFileName:String) {
        ResourceLoader.getResourceAsStream(sampleFileName)?.use {
            (Application.focusedGraphComponent as? SbgnGraphComponent)?.let { graphComponent ->
                graphComponent.graph.clear()
                SbgnReader().read(it, graphComponent.graph, graphComponent)
                ICommand.FIT_GRAPH_BOUNDS.execute(null, graphComponent)
            }
        }
    }

}

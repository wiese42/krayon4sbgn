/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("UNCHECKED_CAST")

package krayon.editor.sbgn.ui

import krayon.editor.base.Application
import krayon.editor.base.ApplicationEvent
import krayon.editor.base.command.ApplicationCommand
import krayon.editor.base.style.GraphStyle
import krayon.editor.base.style.StyleManager
import krayon.editor.base.style.StyleProperty
import krayon.editor.base.ui.*
import krayon.editor.base.util.IconManager
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.graphStyle
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.SbgnBuilder
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel
import kotlin.math.min

private typealias SbgnStyle = GraphStyle<SbgnType>

class StyleManagementToolBar(val styleManager: StyleManager<SbgnType>, val palette: SbgnPaletteComponent, private val propertyTable: PropertyTable) : JToolBar() {
    private val deleteAction: Action
    private val newAction: Action
    private val editAction: Action
    private val styleComboBox: JComboBox<SbgnStyle>

    private val currentStyle: GraphStyle<SbgnType> get() = styleManager.currentStyle!! //shortcut

    init {
        isFloatable = false

        styleComboBox = JComboBox(styleManager.styles.toTypedArray())
        styleComboBox.apply {
            maximumSize = Dimension(min(preferredSize.width, 200), Int.MAX_VALUE)

            selectedItem = currentStyle

            addItemListener { e ->
                if (e.stateChange == ItemEvent.SELECTED) {
                    (e.item as SbgnStyle).let {
                        styleManager.currentStyle = it
                        onCurrentStyleChange(it)
                    }
                }
            }
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                    var renderValue = (value as SbgnStyle).name
                    when {
                        value.isFileLocal -> {
                            renderValue = "File:$renderValue"
                            styleComboBox.toolTipText = "<html>$renderValue<br>This is a transient style defined in an SBGN-ML file.<br> Clone this style to keep it."
                        }
                        value.isReadOnly -> {
                            renderValue += " [Read-Only]"
                            styleComboBox.toolTipText = "<html>$renderValue<br>This is an unmodifiable system style.<br> Clone this style to modify it."
                        }
                        else -> styleComboBox.toolTipText = "<html>$renderValue<br>Modify this style by selecting palette <br> elements and changing values in the properties table"
                    }
                    return super.getListCellRendererComponent(list, renderValue, index, isSelected, cellHasFocus)
                }
            }

        }

        add(styleComboBox)
        //add(Box.createHorizontalGlue())
        //addAction(NextStyleInUse.getAction(Application.focusedGraphComponent!!).apply { icon = NextStyleInUse.icon16 })
        addAction(ApplyStyleToSelection.getAction(Application.focusedGraphComponent!!).apply { icon = ApplyStyleToSelection.icon16 })

        newAction = object : AbstractAction("New") {
            init {
                icon = IconManager.iconMap["ICON.NEW_STYLE"]?.icon16
                tooltip = "Create new style as a clone of the current one."
            }

            override fun actionPerformed(e: ActionEvent?) {
                val customCount = styleManager.styles.count { !it.isReadOnly }
                val name: String? = JOptionPane.showInputDialog(palette, "Name of New Style:", "Custom${customCount + 1}")
                if (name != null) {
                    val newStyle = styleManager.createStyle(name, currentStyle)
                    styleManager.addStyle(newStyle)
                    styleComboBox.addItem(newStyle)
                    styleComboBox.selectedItem = newStyle
                }
            }
        }

        editAction = object : AbstractAction("Edit") {
            init {
                icon = IconManager.iconMap["ICON.EDIT_STYLE"]?.icon16
                selectedIcon = IconManager.iconMap["ICON.EDIT_STYLE"]?.selectedIcon16
                tooltip = "Customize current style."
                isSelected = false
                name = null
            }

            override fun actionPerformed(e: ActionEvent?) {
                styleManager.fireStyleEvent(currentStyle,
                        if (isSelected == true) StyleManager.StyleOp.SHOW_EDITOR else StyleManager.StyleOp.HIDE_EDITOR)
            }
        }

        deleteAction = object : AbstractAction("Delete") {
            init {
                icon = IconManager.iconMap["ICON.DELETE_STYLE"]?.icon16
                tooltip = "Delete current style"
            }

            override fun actionPerformed(e: ActionEvent?) {
                if (JOptionPane.showConfirmDialog(palette, "Irrevocably delete this style?", "Are you Sure?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    styleManager.deleteStyle(currentStyle)
                    styleComboBox.removeItem(currentStyle)
                }
            }
        }
        addAction(newAction)
        addToggleAction(editAction)
        addAction(deleteAction)

        connectPaletteWithPropertyTable(palette, propertyTable)

        styleManager.styleListeners += object:StyleManager.StyleListener<SbgnType> {
            override fun onStyleEvent(graphStyle: GraphStyle<SbgnType>, op: StyleManager.StyleOp) {
                if(op == StyleManager.StyleOp.CURRENT_STYLE_CHANGED) {
                    //println("toolbar.stylelistener>>>")
                    if(styleComboBox.selectedItem != currentStyle) styleComboBox.selectedItem = currentStyle
                    //println("toolbar.stylelistener<<<")
                }
            }
        }

        onCurrentStyleChange(currentStyle)

        updateActionStates()

        Application.applicationListeners += onDiagramLoaded()
    }

    private fun addAction(action:Action) {
        add(JButton(action).apply {
            text = null
            border = emptyBorder(5,5,5,5)
        })
    }

    private fun addToggleAction(action:Action) {
        add(UiFactory.createStateButton(action).apply {
            border = emptyBorder(5,5,5,5)
        })
    }

    private fun onDiagramLoaded() = { event:ApplicationEvent ->
        if (event.type == "DIAGRAM.LOADED") {
            //remove previous file-local styles
            styleManager.styles.filter { style -> style.isFileLocal }.forEach(styleManager::removeStyle)
            if (!styleManager.styles.contains(styleManager.currentStyle)) {
                styleManager.currentStyle = styleManager.getDefaultStyle()
            }

            val graphComponent = Application.focusedGraphComponent as SbgnGraphComponent
            val graph = graphComponent.graph
            val stylableModelItems = (graph.nodes + graph.edges)
            val fileStyles = stylableModelItems.mapNotNull { it.graphStyle }.toMutableSet()
            graphComponent.graphStyle?.let { fileStyles += it }

            //if fileStyle matches managed style replace references in nodes, edges, and graphcomponent
            styleManager.styles.forEach { style ->
                fileStyles.find { it === style || it.styleTemplateMap == style.styleTemplateMap }?.let { fileStyle ->
                    stylableModelItems.forEach {
                        if(it.graphStyle == fileStyle) it.graphStyle = style
                    }
                    if(graphComponent.graphStyle == fileStyle) graphComponent.graphStyle = style
                    fileStyles.remove(fileStyle)
                }
            }
            //add remaining file styles to manager
            fileStyles.forEach(styleManager::addStyle)

            //update combobox items. try no to change selected item.
            (0 until styleComboBox.itemCount).map { styleComboBox.getItemAt(it) }.forEach {
                if (styleComboBox.selectedItem != it) {
                    styleComboBox.removeItem(it)
                }
            }
            val styles = styleManager.getStylesInDisplayOrder()
            val selectedIndex = styles.indexOf(styleComboBox.selectedItem)
            styles.forEachIndexed { index, style ->
                if (index < selectedIndex) styleComboBox.insertItemAt(style, index)
                else if (index > selectedIndex) styleComboBox.addItem(style)
            }

            if(graphComponent.graphStyle != null && graphComponent.graphStyle != currentStyle) {
                styleManager.currentStyle = graphComponent.graphStyle
            }
        }
    }


    private fun onCurrentStyleChange(selectedStyle: SbgnStyle) {
        //println("onCurrentStyleChange")
        palette.clearSelection()
        updatePropertyTable()
        propertyTable.isEnabled = !selectedStyle.isReadOnly
        propertyTable.tableHeader.columnModel.getColumn(1).headerValue = "Value" +  (if(selectedStyle.isReadOnly) " [Read-Only]" else "")
        propertyTable.tableHeader.repaint()
        updateActionStates()
    }

    private fun updateActionStates() {
        deleteAction.isEnabled = !currentStyle.isReadOnly
    }

    private fun updateDiagram() {
        (Application.focusedGraphComponent as SbgnGraphComponent).let { graphComponent ->
            val graph = graphComponent.graph
            graph.nodes.forEach {
                if(it.graphStyle == currentStyle) styleManager.applyStyle(currentStyle, graph, it, false)
            }
            graph.edges.forEach {
                if(it.graphStyle == currentStyle) styleManager.applyStyle(currentStyle, graph, it, false)
            }
            if(graphComponent.graphStyle == currentStyle) graphComponent.applyStyle(currentStyle)
        }
    }

    private fun updatePropertyTable() {
        val styleMaps = mutableListOf<Map<StyleProperty, Any?>>()
        val tableModel = propertyTable.model as DefaultTableModel
        tableModel.rowCount = 0

        with(palette) {
            if (selectedIndices.isEmpty()) {
                val map = mutableMapOf<StyleProperty, Any?>()
                currentStyle.styleTemplateMap[SbgnType.MAP]?.forEach { key, value ->
                    map[key] = value
                }
                styleMaps.add(map)
            }
            selectedIndices.forEach { index ->
                getPaletteModelItem(index)?.type?.let { type ->
                    currentStyle.styleTemplateMap[type]?.let {
                        styleMaps.add(it)
                    }
                }
            }
        }

        StyleProperty.values().forEach { key ->
            if (styleMaps.any { it.containsKey(key) }) {  //only if this key is present in at least one styleMap
                val valueSet = styleMaps.mapNotNull { it[key] }.toSet()  //map if not null
                if (valueSet.size == 1) { //one key, one value
                    tableModel.addRow(arrayOf(key, valueSet.first()))
                } else if (valueSet.size > 1) {
                    tableModel.addRow(arrayOf(key, PropertyTable.UNDEFINED))
                }
            }
        }
    }

    private fun updateCurrentStyle(key:StyleProperty, value:Any?) {
        with(palette) {
            if(selectedIndices.isEmpty()) {
                styleManager.updateStyle(currentStyle, listOf(SbgnType.MAP), key, value)
            }
            else {
                selectedIndices.toList().mapNotNull { getPaletteModelItem(it)?.type }.let {
                    styleManager.updateStyle(currentStyle, it, key, value)
                }
            }
        }
        updateDiagram()
    }

    private fun connectPaletteWithPropertyTable(palette: SbgnPaletteComponent, table: PropertyTable) {
        palette.addListSelectionListener(ListSelectionListener {
            updatePropertyTable()
        })

        table.model.addTableModelListener { e ->
            if(e.type != TableModelEvent.UPDATE) return@addTableModelListener
            val row = e.firstRow
            val key = table.model.getValueAt(row,0) as StyleProperty
            val value = table.model.getValueAt(row,1)
            updateCurrentStyle(key, value)
        }
    }

    companion object {
        val ApplyStyleToSelection = object:ApplicationCommand("APPLY_STYLE_TO_SELECTION") {
            override fun execute(param: Any?) {
                val graphStyle = SbgnBuilder.styleManager.currentStyle!!
                if(graphComponent.selection.none()) {
                    (graphComponent as SbgnGraphComponent).applyStyle(graphStyle)
                }
                else {
                    val selectedItems = graphComponent.selection.selectedNodes + graphComponent.selection.selectedEdges
                    selectedItems.forEach {
                        SbgnBuilder.styleManager.applyStyle(graphStyle, graph, it, false)
                    }
                }
                graphComponent.graph.invalidateDisplays()
            }

            override fun canExecute(param: Any?) = true
        }

        val ApplyStyleToDiagram = object:ApplicationCommand("APPLY_STYLE_TO_DIAGRAM") {
            override fun execute(param: Any?) {
                val graphStyle = SbgnBuilder.styleManager.currentStyle!!
                (graphComponent as SbgnGraphComponent).applyStyle(graphStyle)
                (graphComponent.graph.nodes + graphComponent.graph.edges).forEach {
                    SbgnBuilder.styleManager.applyStyle(graphStyle, graph, it, false)
                }
                graphComponent.graph.invalidateDisplays()
            }
            override fun canExecute(param: Any?) = true
        }

        val NextStyleInUse = object:ApplicationCommand("NEXT_STYLE_IN_USE") {
            override fun canExecute(param: Any?) = true
            override fun execute(param: Any?) {
                val stylesInUse = (graph.nodes + graph.edges).map { it.graphStyle }.toSet()
                val currentStyle = SbgnBuilder.styleManager.currentStyle
                val styles = SbgnBuilder.styleManager.getStylesInDisplayOrder()
                val currentIndex = styles.indexOf(currentStyle)
                val nextStyle = styles.filterIndexed { index, graphStyle -> index > currentIndex && stylesInUse.contains(graphStyle) }.firstOrNull() ?:
                    styles.filterIndexed { index, graphStyle -> index < currentIndex && stylesInUse.contains(graphStyle) }.firstOrNull()
                if(nextStyle != null) {
                    SbgnBuilder.styleManager.currentStyle = nextStyle
                }
            }

        }
    }
}


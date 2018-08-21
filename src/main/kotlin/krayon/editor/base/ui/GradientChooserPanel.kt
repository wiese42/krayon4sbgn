/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import krayon.editor.base.style.OrientedGradientPaint
import java.awt.*
import java.util.*
import javax.swing.*
import javax.swing.colorchooser.ColorSelectionModel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer


class GradientChooserPanel : JPanel() {

    lateinit var gradientPaint:OrientedGradientPaint

    class PreviewComponent: JComponent(), ChangeListener {

        var originalPaint:OrientedGradientPaint? = null
        var colorIndex:Int = -1

        var previewPaint:OrientedGradientPaint? = null

        //get notified when color in individual color selector changes
        override fun stateChanged(e: ChangeEvent) {
            val model = e.source as ColorSelectionModel
            val newColor = model.selectedColor
            if(originalPaint != null && colorIndex >= 0) {
                val colors = originalPaint!!.colors.copyOf()
                colors[colorIndex] = newColor
                previewPaint = OrientedGradientPaint(originalPaint!!.degAngle, originalPaint!!.fractions, colors)
            }
        }

        init {
            isOpaque = true
            preferredSize = Dimension(120,80)
        }

        override fun paintComponent(g: Graphics) {
            g.create { gfx ->
                g.color = Color.WHITE
                g.fillRect(0, 0, width, height)
                if (previewPaint != null && width > 0 && height > 0) {
                    gfx.paint = previewPaint
                    gfx.fillRoundRect((width * 0.1).toInt(), (height * 0.1).toInt(), (width * 0.8).toInt(), (height * 0.8).toInt(), 20, 20)
                }
            }
        }
    }

    private val previewComponent = PreviewComponent()

    private val angleSlider = JSlider(JSlider.HORIZONTAL,0,180,90).apply {
        majorTickSpacing = 45
        //minorTickSpacing = 10
        paintTicks = true
        paintLabels = true
        val tickTable = Hashtable<Int, JLabel>()
        tickTable[0] = JLabel("0°")
        //tickTable[45] = JLabel("45°")
        tickTable[90] = JLabel("90°")
        //tickTable[135] = JLabel("135°")
        tickTable[180] = JLabel("180°")
        labelTable = tickTable
    }

    private val colorTable = GradientColorTable()

    inner class GradientColorTable : JTable() {
        private val colorEditor = PropertyTable.ColorEditor().apply {
            paintChooser.previewPanel = PreviewComponent().apply {
                paintChooser.selectionModel.addChangeListener(this)
            }
        }

        init {
            val model = DefaultTableModel()
            model.addColumn("Fraction")
            model.addColumn("Color")
            this.model = model
        }

        override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
            return when(column) {
                1 -> PropertyTable.PaintRenderer()
                else -> PropertyTable.DoubleRenderer()
            }
        }

        override fun getCellEditor(row: Int, column: Int): TableCellEditor {
            return when(column) {
                1 -> colorEditor.apply {
                    (paintChooser.previewPanel as PreviewComponent).apply {
                        val fraction = (getValueAt(row,0) as Double).toFloat()
                        colorIndex = (0 until gradientPaint.fractions.size).find { gradientPaint.fractions[it] == fraction } ?: -1
                        originalPaint = gradientPaint
                    }
                }
                else -> PropertyTable.DoubleEditor(0.0, 1.0)
            }
        }
    }


    init {
        layout = BorderLayout()

        val tablePane = JScrollPane(colorTable)
        tablePane.preferredSize = Dimension(250,200)


        angleSlider.addChangeListener { updateGradientPaint() }


        val removeButton = JButton("Remove").apply {
            addActionListener {
                if(colorTable.selectedRow >= 0) {
                    (colorTable.model as DefaultTableModel).removeRow(colorTable.selectedRow)
                }
            }
            maximumSize = preferredSize
        }

        val addButton = JButton("Add").apply {
            addActionListener { _ ->
                with((colorTable.model as DefaultTableModel)) {
                    val newRow = arrayOf(0.5, Color.WHITE)
                    val upperRow = (0 until rowCount).firstOrNull { (getValueAt(it,0) as Double) > 0.5  }
                    if(upperRow == null) addRow(newRow)
                    else insertRow(upperRow, newRow)
                }
            }
            maximumSize = removeButton.preferredSize
        }

        val buttonPane = Box(BoxLayout.Y_AXIS)
        buttonPane.add(addButton)
        buttonPane.add(removeButton)
        buttonPane.add(Box.createVerticalGlue())

        val leftPane = Box(BoxLayout.X_AXIS)
        leftPane.add(tablePane)
        leftPane.add(Box.createRigidArea(Dimension(10,10)))
        leftPane.add(buttonPane)

        val rightPane = JPanel()
        rightPane.add(angleSlider)

        val centerPanel = JPanel()
        centerPanel.add(rightPane)
        centerPanel.add(leftPane)
        centerPanel.border = emptyBorder(40,0,0,0)

        previewComponent.preferredSize = Dimension(120,80)
        val previewPanel = JPanel()
        previewPanel.add(previewComponent)
        previewPanel.border = BorderFactory.createTitledBorder("Preview")

        centerPanel.maximumSize = centerPanel.preferredSize

        add(centerPanel, BorderLayout.CENTER)
        add(previewPanel, BorderLayout.SOUTH)

        initialize(OrientedGradientPaint(0.0, floatArrayOf(0f,1f), arrayOf(Color.WHITE, Color.BLACK)))
    }

    private fun updateGradientPaint() {
        val rows = (0 until colorTable.model.rowCount).map { index -> Pair(colorTable.model.getValueAt(index,0) as Double, colorTable.model.getValueAt(index,1) as Color)}.sortedBy { it.first }
        val uniqueRows = mutableListOf<Pair<Double, Color>>()
        var lastValid:Double? = null
        for (row in rows) {
            if(row.first != lastValid) {
                uniqueRows.add(row)
                lastValid = row.first
            }
        }
        val fractions = uniqueRows.map { it.first.toFloat()}.toFloatArray()
        val colors = uniqueRows.map { it.second }.toTypedArray()

        if(colors.size > 1) {
            gradientPaint = OrientedGradientPaint(angleSlider.value.toDouble(), fractions, colors)
        }
        previewComponent.previewPaint = gradientPaint
        previewComponent.repaint()
    }

    fun initialize(initialPaint: OrientedGradientPaint) {
        gradientPaint = initialPaint
        angleSlider.value = initialPaint.degAngle.toInt()
        val colorCount = initialPaint.colors.size
        val model = (colorTable.model as DefaultTableModel).apply {
            while (rowCount > 0) removeRow(rowCount-1)
            (0 until colorCount).forEach { index ->
                addRow(arrayOf(initialPaint.fractions[index].toDouble(), initialPaint.colors[index]))
            }
        }
        colorTable.model = model
        colorTable.model.addTableModelListener { updateGradientPaint() }
    }
}
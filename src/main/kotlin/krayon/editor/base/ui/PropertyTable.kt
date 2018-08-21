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
import krayon.editor.base.style.FontStyleValue
import krayon.editor.base.style.OrientedGradientPaint
import krayon.editor.base.style.StateVariableShapeValue
import krayon.editor.base.style.StyleProperty
import java.awt.*
import java.awt.event.*
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.text.DefaultFormatter
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter

class PropertyTable : JTable() {

    private val nameRenderer = PropertyKeyRenderer()

    private val paintRenderer = PaintRenderer()
    private val paintEditor = PaintEditor()

    private val colorEditor = ColorEditor()
    private val doubleRenderer = DoubleRenderer()
    private val doubleEditor = DoubleEditor()

    private val booleanRenderer = BooleanRenderer()
    private val booleanEditor = BooleanEditor()

    private val enumRenderer = EnumRenderer()

    private val insetsEditor = InsetsEditor()
    private val insetsRenderer = InsetsRenderer()

    init {
        val model = DefaultTableModel()

        model.addColumn("Name")
        model.addColumn("Value")
        this.model = model
        tableHeader.reorderingAllowed = false
    }

    override fun isCellEditable(row: Int, column: Int) = column == 1

    override fun getCellRenderer(row: Int, column: Int): TableCellRenderer {
        return if(column == 0) nameRenderer
        else when(getValueAt(row,column)) {
            is Paint -> paintRenderer
            is Double -> doubleRenderer
            is Boolean -> booleanRenderer
            is InsetsD -> insetsRenderer
            else -> enumRenderer
            //else -> (super.getCellRenderer(row, column) as DefaultTableCellRenderer).apply {
            //    horizontalAlignment = JLabel.RIGHT
            //}
        }
    }

    override fun getCellEditor(row: Int, column: Int): TableCellEditor {
        return when((getValueAt(row,0) as StyleProperty).valueType) {
            Color::class.java -> colorEditor
            Paint::class.java -> paintEditor
            Double::class.java -> doubleEditor
            Boolean::class.java -> booleanEditor
            InsetsD::class.java -> insetsEditor
            FontStyleValue::class.java -> EnumEditor(JComboBox(FontStyleValue.values()).apply { selectedItem = getValueAt(row, 1) })
            StateVariableShapeValue::class.java -> EnumEditor(JComboBox(StateVariableShapeValue.values()).apply { selectedItem = getValueAt(row, 1) })
            else -> super.getCellEditor(row, column)
        }
    }

    object UNDEFINED {
        override fun toString(): String {
            return "Undefined"
        }
    }

    class InsetsRenderer:DefaultTableCellRenderer() {
        init {
            horizontalAlignment = JLabel.RIGHT
        }
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            val renderValue = if (value is InsetsD) {
                "T R B L = ${value.top.toInt()} ${value.right.toInt()} ${value.bottom.toInt()} ${value.left.toInt()}"
            } else value
            return super.getTableCellRendererComponent(table, renderValue, isSelected, hasFocus, row, column)
        }
    }

    //remember: T R B L
    class InsetsEditor:DefaultCellEditor(JFormattedTextField()) {
        var ftf:JFormattedTextField = component as JFormattedTextField
        init{
            //ftf.formatterFactory = DefaultFormatterFactory(doubleFormatter)
            ftf.value = InsetsD(1.0,2.0,3.0,4.0)
            ftf.horizontalAlignment = JTextField.TRAILING
            ftf.focusLostBehavior = JFormattedTextField.PERSIST
            ftf.formatterFactory = DefaultFormatterFactory(object:DefaultFormatter() {
                override fun stringToValue(string: String): Any {
                     try {
                        val intList =  string.split(" ").map { it.toInt() }
                        if(intList.size == 1) {
                            val top = intList[0].toDouble()
                            return InsetsD.fromLTRB(top,top,top,top)
                        }
                        else if(intList.size == 4) {
                            val top = intList[0].toDouble()
                            val right = intList[1].toDouble()
                            val bottom = intList[2].toDouble()
                            val left = intList[3].toDouble()
                            return InsetsD.fromLTRB(left, top, right, bottom)
                        }
                    } catch (ex: NumberFormatException) {}
                    throw ParseException("cannot parse $string as inset",0)
                }

                override fun valueToString(value: Any): String {
                    return if(value is InsetsD) {
                        "${value.top.toInt()} ${value.right.toInt()} ${value.bottom.toInt()} ${value.left.toInt()}"
                    } else super.valueToString(value)
                }
            })

            //React when the user presses Enter while the editor is
            //active.  (Tab is handled as specified by
            //JFormattedTextField's focusLostBehavior property.)
            ftf.inputMap.put(KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0), "check")
            ftf.actionMap.put("check", object:AbstractAction() {
                override fun actionPerformed(e:ActionEvent) {
                    if (!ftf.isEditValid) {
                        if (userSaysRevert()) ftf.postActionEvent()
                    }
                    else try {
                        ftf.commitEdit()     //so use it.
                        ftf.postActionEvent() //stop editing
                    } catch (exc:java.text.ParseException) {}
                }
            })

            ftf.addFocusListener(object:FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    super.focusGained(e)
                    ftf.caretPosition = ftf.document.length
                    SwingUtilities.invokeLater { ftf.selectAll() }
                }
            })

        }

        //Override to invoke setValue on the formatted text field.
        override fun getTableCellEditorComponent(table:JTable,
                                                 value:Any, isSelected:Boolean,
                                                 row:Int, column:Int):Component {
            ftf.border = emptyBorder(0)
            if(value is InsetsD) ftf.value = value
            else {
                ftf.value = InsetsD.EMPTY
            }
            return ftf
        }

        //Override to ensure that the value remains an Inset
        override fun getCellEditorValue():Any? {
            val ftf = component as JFormattedTextField
            val o = ftf.value
            return o as? InsetsD
        }

        //Override to check whether the edit is valid,
        //setting the value if it is and complaining if
        //it isn't.  If it's OK for the editor to go
        //away, we need to invoke the superclass's version
        //of this method so that everything gets cleaned up.
        override fun stopCellEditing():Boolean {
            val ftf = component as JFormattedTextField
            if (ftf.isEditValid)
            {
                try
                {
                    ftf.commitEdit()
                }
                catch (exc:java.text.ParseException) {}

            }
            else
            { //text is invalid
                if (!userSaysRevert())
                { //user wants to edit
                    return false //don't let the editor go away
                }
            }
            return super.stopCellEditing()
        }

        private fun userSaysRevert():Boolean {
            ftf.selectAll()
            val options = arrayOf<Any>("Edit", "Revert")
            val answer = JOptionPane.showOptionDialog(
                    SwingUtilities.getWindowAncestor(ftf),
                    "Wrong Format. Must be either one integer or four integers separated by whitespace",
                    "Invalid Text Entered",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE, null,
                    options,
                    options[1])

            if (answer == 1)
            { //Revert!
                ftf.value = ftf.value
                return true
            }
            return false
        }
    }


    class EnumRenderer:DefaultTableCellRenderer() {
        init {
            horizontalAlignment = JLabel.RIGHT
        }
    }

    class EnumEditor<T:Enum<T>>(comboBox:JComboBox<T>):DefaultCellEditor(comboBox) {
        init {
            (comboBox.renderer as? JLabel)?.let { it.horizontalAlignment = JLabel.RIGHT }
        }
    }

    class DoubleRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = SwingConstants.RIGHT
        }
    }

    class BooleanRenderer : JCheckBox(), TableCellRenderer {
        init {
            horizontalAlignment = JLabel.RIGHT
            //isBorderPainted = true
            isOpaque = true
        }

        override fun getTableCellRendererComponent(table: JTable, value: Any?,
                                                   isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            //what a glorious hack/workaround
            val bg = getRowBackground(table, row, isSelected)
            background = Color(bg.red, bg.green, if(bg.blue > 128) bg.blue-1 else bg.blue+1)

            setSelected(value != null && (value == true))
            return this
        }
    }

    class PropertyKeyRenderer:DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            val name = (value as StyleProperty).displayName
            return super.getTableCellRendererComponent(table, name, isSelected, hasFocus, row, column).apply {
                border = emptyBorder(0,5,0,0)
            }
        }
    }

    class BooleanEditor:DefaultCellEditor(JCheckBox().apply { horizontalAlignment = SwingConstants.RIGHT })

    class DoubleEditor(private val minimum:Double? = null, private val maximum:Double? = null):DefaultCellEditor(JFormattedTextField()) {
        var ftf:JFormattedTextField = component as JFormattedTextField
        private var doubleFormat:NumberFormat = DecimalFormat.getInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 1
        }

        init{
            //Set up the editor for the integer cells.
            val doubleFormatter = NumberFormatter(doubleFormat)
            doubleFormatter.format = doubleFormat
            if(minimum != null) doubleFormatter.minimum = minimum
            if(maximum != null) doubleFormatter.maximum = maximum

            //ftf.formatterFactory = DefaultFormatterFactory(doubleFormatter)
            ftf.value = 0.0
            ftf.horizontalAlignment = JTextField.TRAILING
            ftf.focusLostBehavior = JFormattedTextField.PERSIST
            ftf.formatterFactory = DefaultFormatterFactory(NumberFormatter(doubleFormat).apply {
                this.minimum = minimum
                this.maximum = maximum
            })

            //React when the user presses Enter while the editor is
            //active.  (Tab is handled as specified by
            //JFormattedTextField's focusLostBehavior property.)
            ftf.inputMap.put(KeyStroke.getKeyStroke(
                    KeyEvent.VK_ENTER, 0),
                    "check")
            ftf.actionMap.put("check", object:AbstractAction() {
                override fun actionPerformed(e:ActionEvent) {
                    if (!ftf.isEditValid)
                    { //The text is invalid.
                        if (userSaysRevert())
                        { //reverted
                            ftf.postActionEvent() //inform the editor
                        }
                    }
                    else
                        try
                        {              //The text is valid,
                            ftf.commitEdit()     //so use it.
                            ftf.postActionEvent() //stop editing
                        }
                        catch (exc:java.text.ParseException) {}

                }
            })

            ftf.addFocusListener(object:FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    super.focusGained(e)
                    ftf.caretPosition = ftf.document.length
                    SwingUtilities.invokeLater { ftf.selectAll() }
                }
            })

        }

        //Override to invoke setValue on the formatted text field.
        override fun getTableCellEditorComponent(table:JTable,
                                                 value:Any, isSelected:Boolean,
                                                 row:Int, column:Int):Component {
            ftf.border = emptyBorder(0)
            if(value is Number) ftf.value = value
            else {
                ftf.value = 0.0
            }
            return ftf
        }

        //Override to ensure that the value remains a Double
        override fun getCellEditorValue():Any? {
            val ftf = component as JFormattedTextField
            val o = ftf.value
            return o as? Double ?: (o as? Number)?.toDouble()
        }

        //Override to check whether the edit is valid,
        //setting the value if it is and complaining if
        //it isn't.  If it's OK for the editor to go
        //away, we need to invoke the superclass's version
        //of this method so that everything gets cleaned up.
        override fun stopCellEditing():Boolean {
            val ftf = component as JFormattedTextField
            if (ftf.isEditValid)
            {
                try
                {
                    ftf.commitEdit()
                }
                catch (exc:java.text.ParseException) {}

            }
            else
            { //text is invalid
                if (!userSaysRevert())
                { //user wants to edit
                    return false //don't let the editor go away
                }
            }
            return super.stopCellEditing()
        }

        private fun userSaysRevert():Boolean {
            ftf.selectAll()
            val options = arrayOf<Any>("Edit", "Revert")
            val answer = JOptionPane.showOptionDialog(
                    SwingUtilities.getWindowAncestor(ftf),
                    "Value out of bounds. Must be between $minimum and $maximum",
                    "Invalid Text Entered",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE, null,
                    options,
                    options[1])

            if (answer == 1)
            { //Revert!
                ftf.value = ftf.value
                return true
            }
            return false
        }
    }

    class PaintIcon : Icon {
        var paint:Paint? = null
        var width:Int = 10
        var height:Int = 10

        override fun getIconHeight(): Int {
            return height
        }

        override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
            (g as Graphics2D).paint = paint
            g.fillRect(x,y,iconWidth, iconHeight)
        }

        override fun getIconWidth(): Int {
            return width
        }
    }

    class PaintRenderer : DefaultTableCellRenderer() {
        private var paintIcon:PaintIcon = PaintIcon()
        init {
            isOpaque = true
            horizontalAlignment = JLabel.RIGHT
        }

        override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
            background = getRowBackground(table,row,isSelected)

            text = ""
            icon = null
            if(value == UNDEFINED) {
                toolTipText = "Value of selected items differ."
                text = value.toString()
                icon = null
            }
            else {
                if(value is Color) {
                    toolTipText = "RGB value: " + value.red + ", " + value.green + ", " + value.blue
                }
                else if(value is OrientedGradientPaint) {
                    toolTipText = "Oriented Gradient"
                }
                paintIcon.apply {
                    this.width = 50
                    this.height = (table.getRowHeight(row)*0.6).toInt() //50
                    this.paint = value as? Paint
                }
                icon = paintIcon
            }

            return this
        }
    }

    class ColorEditor : AbstractCellEditor(), TableCellEditor {

        var currentPaint: Paint? = null
        var paintChooser: JColorChooser = JColorChooser()
        private var dialog: JDialog
        private val renderer: PaintRenderer = PaintRenderer()

        init {
            dialog = JColorChooser.createDialog(renderer, "Color Picker", true, paintChooser,
                    ActionListener { _ -> paintChooser.color?.let { currentPaint = it } },
                    null)

            renderer.addMouseListener(object:MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if(currentPaint is Paint) {
                        paintChooser.color = currentPaint as Color
                    }
                    dialog.isVisible = true
                    fireEditingStopped() //Make the renderer reappear.
                }})
        }

        override fun getCellEditorValue(): Any? {
            return currentPaint
        }

        override fun getTableCellEditorComponent(table: JTable,
                                                 value: Any,
                                                 isSelected: Boolean,
                                                 row: Int,
                                                 column: Int): Component {
            currentPaint = value as? Paint
            return renderer.getTableCellRendererComponent(table, value, true, true, row, column)
        }
    }

    class PaintEditor : AbstractCellEditor(), TableCellEditor {

        var currentPaint: Paint? = null
        private var paintChooser: JPaintChooser = JPaintChooser()
        private var dialog: JDialog
        private val renderer: PaintRenderer = PaintRenderer()

        init {
            dialog = JPaintChooser.createDialog(renderer, "Color Picker", true, paintChooser,
                    ActionListener { _ -> paintChooser.getPaint()?.let { currentPaint = it } },
                    null)

            renderer.addMouseListener(object:MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    if(currentPaint is Paint) {
                        paintChooser.setPaint(currentPaint as Paint)
                    }
                    dialog.isVisible = true
                    fireEditingStopped() //Make the renderer reappear.
                }})
        }

        override fun getCellEditorValue(): Any? {
            return currentPaint
        }

        override fun getTableCellEditorComponent(table: JTable,
                                                 value: Any,
                                                 isSelected: Boolean,
                                                 row: Int,
                                                 column: Int): Component {
            currentPaint = value as? Paint
            return renderer.getTableCellRendererComponent(table, value, true, true, row, column)
        }
    }

    companion object {
        fun getRowBackground(table:JTable, row:Int, isSelected:Boolean):Color {
            return if(isSelected) UIManager.get("nimbusSelectionBackground") as? Color ?: table.selectionBackground
            else {
                if(row % 2 == 1) UIManager.get("Table.alternateRowColor") as? Color ?:  UIManager.get("Table.background") as? Color ?: table.background
                else  UIManager.get("Table.background") as? Color ?: table.background
            }
        }

    }
}


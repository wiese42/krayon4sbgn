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
import java.awt.event.ActionListener
import javax.swing.*

class JPaintChooser : JPanel(BorderLayout()) {
    private val colorChooser = JColorChooser()
    private val gradientChooser = GradientChooserPanel()
    private val colorButton:JRadioButton
    private val gradientButton:JRadioButton
    private val cardLayout = CardLayout()
    private val cardPanel: JPanel

    init {

        cardPanel = JPanel(cardLayout).apply {
            add(colorChooser,"Color")
            add(gradientChooser, "Gradient")
        }
        colorButton = JRadioButton("Color").apply {
            addActionListener {
                cardLayout.show(cardPanel, "Color")
            }
            isSelected = true
        }
        gradientButton = JRadioButton("Gradient").apply {
            addActionListener {
                cardLayout.show(cardPanel, "Gradient")
            }
        }

        ButtonGroup().apply {
            add(colorButton)
            add(gradientButton)
        }

        val topPanel = JPanel().apply {
            add(colorButton)
            add(gradientButton)
        }

        add(topPanel, BorderLayout.NORTH)
        add(cardPanel, BorderLayout.CENTER)


    }

    fun setPaint(initialPaint:Paint) {
        if(initialPaint is Color) {
            colorChooser.color = initialPaint
            colorButton.isSelected = true
            cardLayout.show(cardPanel, "Color")
        }
        else if(initialPaint is OrientedGradientPaint) {
            gradientChooser.initialize(initialPaint)
            gradientButton.isSelected = true
            cardLayout.show(cardPanel, "Gradient")
        }
    }

    fun getPaint():Paint? {
        return if(colorButton.isSelected) return colorChooser.color
        else gradientChooser.gradientPaint
    }

    companion object {

        fun createDialog(component: Component, title:String, isModal:Boolean, paintChooser:JPaintChooser, okHandler:ActionListener, cancelHandler:ActionListener?): JDialog {
            val window = SwingUtilities.windowForComponent(component)
            val dialog = JDialog(window,title,if(isModal) Dialog.ModalityType.APPLICATION_MODAL else Dialog.ModalityType.MODELESS)

            val buttonPanel = JPanel().apply {
                add(JButton("Ok").apply {
                    addActionListener(okHandler)
                    addActionListener { dialog.dispose() }
                })
                add(JButton("Cancel").apply {
                    if(cancelHandler != null) addActionListener(cancelHandler)
                    addActionListener { dialog.dispose() }
                })
            }

            dialog.apply {
                contentPane = JPanel(BorderLayout())
                contentPane.add(paintChooser, BorderLayout.CENTER)
                contentPane.add(buttonPanel, BorderLayout.SOUTH)
                pack()
                dialog.setLocationRelativeTo(component)
            }

            return dialog
        }

        fun showDialog(component: Component, title:String, initialPaint: Paint):Paint? {
            val paintChooser = JPaintChooser().apply {
                setPaint(initialPaint)
            }

            var returnValue:Paint? = null

            val okHandler = ActionListener {
                returnValue = paintChooser.getPaint()
            }

            val dialog = createDialog(component, title, true, paintChooser, okHandler, ActionListener{})

            dialog.isVisible = true

            return returnValue
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val frame = JFrame()
            val button = JButton("Choose Paint")
            //val initialPaint = OrientedGradientPaint(0.0, floatArrayOf(0f, 0.5f, 1.0f), arrayOf(Color.RED, Color.WHITE, Color.GREEN))
            val initialPaint = Color.RED

            button.addActionListener {
                val paint = JPaintChooser.showDialog(button, "Choose a color", initialPaint)
                println("paint" + paint)
            }
            frame.contentPane.add(button)

            frame.pack()
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.isVisible = true
        }
    }
}
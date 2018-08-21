/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import krayon.editor.base.command.SetLookAndFeel
import krayon.editor.base.ui.SettingAgent
import krayon.editor.base.ui.SettingsDialog
import krayon.editor.base.util.ApplicationSettings
import krayon.editor.sbgn.style.SbgnBuilder
import krayon.util.ResourceLoader
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode

class SbgnSettingsDialog(frame: JFrame?) : SettingsDialog(frame) {

    override fun initializeTree() {
        root.apply {
            add(DefaultMutableTreeNode(LookAndFeelSetting()))
            add(DefaultMutableTreeNode(DefaultStyleSetting()))
        }
    }

    class LookAndFeelSetting : SettingAgent("UI Style") {

        private var selectedLaf: SetLookAndFeel.LnF? = null
        private val previewComponent = JLabel()

        override fun getSettingsComponent(context: SettingsDialog.SettingsContext): JComponent {
            return JPanel(BorderLayout()).apply {

                val buttonPanel = JPanel()
                val buttonGroup = ButtonGroup()

                SetLookAndFeel.LnF.values().forEach { laf ->
                    val button = JRadioButton(laf.getDisplayName())
                    button.addActionListener {
                        selectedLaf = laf
                        updatePreview(laf)
                    }
                    buttonGroup.add(button)
                    buttonPanel.add(button)
                    if(laf == SetLookAndFeel.activeLaF) button.isSelected = true
                }
                add(buttonPanel, BorderLayout.NORTH)
                add(previewComponent, BorderLayout.CENTER)
                updatePreview(SetLookAndFeel.activeLaF)
            }
        }

        private fun updatePreview(laf: SetLookAndFeel.LnF) {
            ResourceLoader.getResource("/resources/laf-preview/laf-${laf.name.toLowerCase()}.png")?.let {
                previewComponent.icon = ImageIcon(it)
                previewComponent.revalidate()
                previewComponent.repaint()
            }
        }

        override fun commitSettings(context: SettingsDialog.SettingsContext) {
            if(selectedLaf != null && selectedLaf != SetLookAndFeel.activeLaF) {
                ApplicationSettings.LOOK_AND_FEEL.value = selectedLaf!!.name
                JOptionPane.showMessageDialog(context.dialog.contentPane,
                        "Please restart the application for this change to take effect.")
            }
        }

        override fun resetSettings(context: SettingsDialog.SettingsContext) {
            //unclear if needed
        }

    }

    class DefaultStyleSetting : SettingAgent("Default SBGN Style") {

        private var styleComboBox: JComboBox<String>? = null
        private var initialValue:String? = null
        override fun getSettingsComponent(context: SettingsDialog.SettingsContext): JComponent {
            return JPanel(BorderLayout()).apply {
                val styleManager = SbgnBuilder.styleManager
                val styles = styleManager.getStylesInDisplayOrder()
                styleComboBox = JComboBox(styles.toList().map { it.name }.toTypedArray()).apply {
                    toolTipText = "The initially selected SBGN style when the application starts."
                }

                val defaultStyle = (styles.firstOrNull { it.name == ApplicationSettings.DEFAULT_SBGN_STYLE.value } ?: styleManager.getDefaultStyle()).name
                styleComboBox?.selectedItem = defaultStyle
                initialValue = defaultStyle

                return JPanel(BorderLayout()).apply {
                    add(styleComboBox, BorderLayout.NORTH)
                }

            }
        }

        override fun commitSettings(context: SettingsDialog.SettingsContext) {
            val selectedName = styleComboBox?.selectedItem
            if(selectedName != null && selectedName != ApplicationSettings.DEFAULT_SBGN_STYLE.value) {
                ApplicationSettings.DEFAULT_SBGN_STYLE.value = selectedName
                JOptionPane.showMessageDialog(context.dialog.contentPane,
                        "Please restart the application for this change to take effect.")
            }
        }

        override fun resetSettings(context: SettingsDialog.SettingsContext) {
            styleComboBox?.selectedItem = initialValue
        }

    }
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import krayon.editor.base.ui.emptyBorder
import krayon.editor.base.util.ApplicationSettings
import krayon.editor.base.util.IconManager
import krayon.editor.base.util.IconManager.iconMapPath
import krayon.util.ResourceLoader
import mdlaf.MaterialLookAndFeel
import mdlaf.resources.MaterialBorders
import mdlaf.resources.MaterialColors
import mdlaf.resources.MaterialFonts
import mdlaf.resources.MaterialImages
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.UIManager

object SetLookAndFeel : ApplicationCommand("SET_LOOK_AND_FEEL") {

    lateinit var activeLaF:LnF
    enum class LnF {
        SYSTEM,
        DARK,
        MATERIAL;
        fun getDisplayName():String {
            return name.substring(0,1) + name.substring(1).toLowerCase()
        }
    }

    override fun canExecute(param: Any?): Boolean {
        return true
    }

    override fun execute(param: Any?) {

        val selectedLaF = LnF.values().firstOrNull { it.name.toLowerCase() == System.getProperty("laf")?.toLowerCase() } ?:
                LnF.values().firstOrNull { ApplicationSettings.LOOK_AND_FEEL.value == it.name } ?:
                param as? LnF ?:
                LnF.values().first()


        if(selectedLaF == LnF.SYSTEM) {
            initializeIcons("default")
            UIManager.put("OptionPane.informationIcon", IconManager.iconMap["OPTIONPANE.INFORMATION"]?.icon32)
            UIManager.put("OptionPane.errorIcon", IconManager.iconMap["OPTIONPANE.ERROR"]?.icon32)
            UIManager.put("OptionPane.questionIcon", IconManager.iconMap["OPTIONPANE.QUESTION"]?.icon32)
            UIManager.put("OptionPane.warningIcon", IconManager.iconMap["OPTIONPANE.WARNING"]?.icon32)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }
        else if(selectedLaF == LnF.DARK) {
            initializeIcons("white")
            UIManager.put("control", Color(70, 70, 70)) //Color(128, 128, 128)
            UIManager.put("info", Color(70, 70, 70)) //Color(128, 128, 128)
            UIManager.put("nimbusBase", Color(18, 30, 49))
            UIManager.put("nimbusAlertYellow", Color(248, 187, 0))
            UIManager.put("nimbusDisabledText", Color(128, 128, 128))
            UIManager.put("nimbusFocus", Color(115, 164, 209))
            UIManager.put("nimbusGreen", Color(176, 179, 50))
            UIManager.put("nimbusInfoBlue", Color(66, 139, 221))
            UIManager.put("nimbusLightBackground", Color(63, 63, 63)) //Color(18, 30, 49))
            UIManager.put("nimbusOrange", Color(191, 98, 4))
            UIManager.put("nimbusRed", Color(169, 46, 34))
            UIManager.put("nimbusSelectedText", Color(255, 255, 255))
            UIManager.put("nimbusSelectionBackground", Color(66, 139, 221)) //Color(104, 93, 156))
            UIManager.put("text", Color(230, 230, 230))
            UIManager.put("Table.foreground", Color(230, 230, 230))
            UIManager.put("Table.alternateRowColor", Color(70,70,70))
            
            UIManager.put("OptionPane.informationIcon", IconManager.iconMap["OPTIONPANE.INFORMATION"]?.icon32)
            UIManager.put("OptionPane.errorIcon", IconManager.iconMap["OPTIONPANE.ERROR"]?.icon32)
            UIManager.put("OptionPane.questionIcon", IconManager.iconMap["OPTIONPANE.QUESTION"]?.icon32)
            UIManager.put("OptionPane.warningIcon", IconManager.iconMap["OPTIONPANE.WARNING"]?.icon32)
            
            for (info in javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus" == info.name) {
                    javax.swing.UIManager.setLookAndFeel(info.className)
                    break
                }
            }
        }
        else if(selectedLaF == LnF.MATERIAL) {
            initializeIcons("default")
            //val selectionColor = MaterialColors.LIGHT_BLUE_400
            //val selectionColor = MaterialColors.GRAY_100)
            val selectionColor = ApplicationSettings.DEFAULT_HIGHLIGHT_COLOR.value as? Color ?: Color.LIGHT_GRAY
            UIManager.put("MenuItem.font", MaterialFonts.REGULAR)
            UIManager.put("Button.background", MaterialColors.GRAY_100)
            UIManager.put("Button.foreground", MaterialColors.GRAY_800)
            UIManager.put("Button.border", emptyBorder(5, 12, 5, 12))
            UIManager.put("ToolBar.background", MaterialColors.GRAY_100)
            UIManager.put("ToolBar.border", MaterialBorders.DEFAULT_SHADOW_BORDER)
            UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder())
            UIManager.put("SplitPane.background",MaterialColors.GRAY_200)
            UIManager.put("SplitPane.dividerSize", 2)
            UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder())
            UIManager.put("Table.rowHeight", 32)
            UIManager.put("Table.selectionBackground", selectionColor)
            UIManager.put("ComboBox.buttonBackground", MaterialColors.WHITE)
            UIManager.put("ComboBox.buttonIcon", ImageIcon(MaterialImages.DOWN_ARROW))
            UIManager.put("Slider.foreground", MaterialColors.GRAY_600)
            UIManager.put("Slider.border", emptyBorder(0))
            UIManager.put("Spinner.previousButtonIcon", ImageIcon(MaterialImages.DOWN_ARROW))
            UIManager.put("Spinner.nextButtonIcon", ImageIcon(MaterialImages.UP_ARROW))
            UIManager.put("Tree.selectionBackground", selectionColor)
            UIManager.put("OptionPane.informationIcon", IconManager.iconMap["OPTIONPANE.INFORMATION"]?.icon32)
            UIManager.put("OptionPane.errorIcon", IconManager.iconMap["OPTIONPANE.ERROR"]?.icon32)
            UIManager.put("OptionPane.questionIcon", IconManager.iconMap["OPTIONPANE.QUESTION"]?.icon32)
            UIManager.put("OptionPane.warningIcon", IconManager.iconMap["OPTIONPANE.WARNING"]?.icon32)

            UIManager.put("Menu.font", MaterialFonts.REGULAR)
            UIManager.put("Menu.border", BorderFactory.createEmptyBorder(5, 0, 5, 5))

            UIManager.put("MenuItem.font", MaterialFonts.REGULAR)
            UIManager.put("MenuItem.border", BorderFactory.createEmptyBorder(5, 0, 5, 0))
            UIManager.put("MenuItem.acceleratorForeground", MaterialColors.BLUE_GRAY_800)

            UIManager.put("RadioButtonMenuItem.font", MaterialFonts.REGULAR)
            UIManager.put("RadioButtonMenuItem.disabledForeground", Color(0, 0, 0, 100))
            UIManager.put("RadioButtonMenuItem.selectionBackground", MaterialColors.GRAY_200)
            UIManager.put("RadioButtonMenuItem.selectionForeground", Color.BLACK)
            UIManager.put("RadioButtonMenuItem.border", BorderFactory.createEmptyBorder(2, 0, 2, 5))
            UIManager.put("RadioButtonMenuItem.background", Color.WHITE)
            UIManager.put("RadioButtonMenuItem.foreground", Color.BLACK)
//
            UIManager.setLookAndFeel(MaterialLookAndFeel())
        }
        activeLaF = selectedLaF
    }

    private fun initializeIcons(theme:String) {
        IconManager.iconPath16 = "/resources/icons/$theme/16"
        IconManager.iconPath32 = "/resources/icons/$theme/32"
        if(iconMapPath != null) {
            ResourceLoader.getResourceAsStream(iconMapPath!!)?.use {
                IconManager.initializeIconResources(it)
            }
        }
    }
}

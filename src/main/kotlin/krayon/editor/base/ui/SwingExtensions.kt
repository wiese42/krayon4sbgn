/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

@file:Suppress("unused")

package krayon.editor.base.ui

import javafx.application.Platform
import javafx.stage.FileChooser
import krayon.util.OperatingSystemChecker
import krayon.util.UnicodeChars
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.geom.Rectangle2D
import java.util.function.Supplier
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

fun JFileChooser.showOpenDialogFX(parent: Component?):Int {
    if(parent != null && (SwingUtilities.getWindowAncestor(parent) as? JFrame)?.isUndecorated == true) {
        return showOpenDialog(parent)
    }
    else try {
        @Suppress("UNUSED_VARIABLE")
        val dummy = javafx.embed.swing.JFXPanel()

        if(Platform.isImplicitExit()) Platform.setImplicitExit(false)

        val fxChooser = SynchronousJFXFileChooser(Supplier {
            val fxChooser = FileChooser()
            fxChooser.title = dialogTitle
            fxChooser.initialDirectory = currentDirectory
            choosableFileFilters.forEach { ff ->
                val eff = ff as ExtensionFileFilter
                fxChooser.extensionFilters.add(FileChooser.ExtensionFilter(eff.description, eff.extensions.map { "*.$it" }))
            }
            fxChooser
        })

        val file = fxChooser.showOpenDialog()

        return if(file != null) {
            selectedFile = file
            JFileChooser.APPROVE_OPTION
        } else JFileChooser.CANCEL_OPTION
    }
    catch (throwable:Throwable) {
        return showOpenDialog(parent)
    }
}

fun JFileChooser.showSaveDialogFX(parent: Component?):Int {
    if(parent != null && (SwingUtilities.getWindowAncestor(parent) as? JFrame)?.isUndecorated == true) {
        return showSaveDialog(parent)
    }
    else try {
        @Suppress("UNUSED_VARIABLE")
        val dummy = javafx.embed.swing.JFXPanel()
        if (Platform.isImplicitExit()) Platform.setImplicitExit(false)

        val fxChooser = SynchronousJFXFileChooser(Supplier {
            val fxChooser = FileChooser()
            fxChooser.title = dialogTitle
            if (selectedFile != null) {
                fxChooser.initialDirectory = selectedFile.parentFile
                fxChooser.initialFileName = selectedFile.name
            } else fxChooser.initialDirectory = currentDirectory

            choosableFileFilters.forEach { ff ->
                val eff = ff as ExtensionFileFilter
                fxChooser.extensionFilters.add(FileChooser.ExtensionFilter(eff.description, eff.extensions.map { "*.$it" }))
            }
            fxChooser
        })

        val file = fxChooser.showSaveDialog()

        return if (file != null) {
            selectedFile = file
            JFileChooser.APPROVE_OPTION
        } else JFileChooser.CANCEL_OPTION
    }
    catch(throwable:Throwable) {
        return showSaveDialog(parent)
    }
}

fun emptyBorder(border:Int) = BorderFactory.createEmptyBorder(border, border, border, border)!!
fun emptyBorder(top:Int, left:Int, bottom:Int, right:Int) = BorderFactory.createEmptyBorder(top, left, bottom, right)!!
fun emptyBorder(insets: Insets) = BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right)!!

var Action.tooltip:String?
    get() = getValue(Action.SHORT_DESCRIPTION) as? String
    set(value) = putValue(Action.SHORT_DESCRIPTION, value)

var Action.icon: Icon?
    get() = getValue(Action.SMALL_ICON) as? Icon
    set(value) = putValue(Action.SMALL_ICON, value)

val Action.SMALL_SELECTED_ICON: String
    get() = "___SELECTED_SELECTED_ICON___"

var Action.selectedIcon: Icon?
    get() = getValue(SMALL_SELECTED_ICON) as? Icon
    set(value) = putValue(SMALL_SELECTED_ICON, value)


var Action.iconPath: String?
    get() = getValue("___ICON_PATH___") as? String
    set(value) {
        putValue("___ICON_PATH___", value)
        icon = ImageIcon(javaClass.getResource(value))
    }

var Action.selectedIconPath: String?
    get() = getValue("___SELECTED_ICON_PATH___") as? String
    set(value) {
        putValue("___SELECTED_ICON_PATH___", value)
        putValue(SMALL_SELECTED_ICON, ImageIcon(javaClass.getResource(value)))
    }

val Action.shortcutString: String
    get() {
        return if (shortcut != null) "[ ${shortcut!!.toShortcutString()} ]" else ""
    }

var Action.name: String?
    get() = getValue(Action.NAME) as? String
    set(value) = putValue(Action.NAME, value)

var Action.shortcut:  KeyStroke?
    get() = getValue(Action.ACCELERATOR_KEY) as? KeyStroke
    set(value) = putValue(Action.ACCELERATOR_KEY, value)

var Action.isSelected:  Boolean?
    get() = getValue(Action.SELECTED_KEY) as? Boolean
    set(value) = putValue(Action.SELECTED_KEY, value)

fun Graphics.create(block:(gfx:Graphics2D) -> Unit) {
    val gfx = create() as Graphics2D
    try { block(gfx) } finally { gfx.dispose() }
}

fun initSwingLookAndFeel() {
    try {
        if ("com.sun.java.swing.plaf.motif.MotifLookAndFeel" != UIManager.getSystemLookAndFeelClassName()
                && "com.sun.java.swing.plaf.gtk.GTKLookAndFeel" != UIManager.getSystemLookAndFeelClassName()
                && UIManager.getSystemLookAndFeelClassName() != UIManager.getLookAndFeel().javaClass.name) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun rectangle2D(x:Double, y:Double, width:Double, height:Double) = Rectangle2D.Double(x,y,width, height)


fun KeyStroke.toShortcutString():String {
    val keyModifierList = mutableListOf<String>()
    if(modifiers and InputEvent.ALT_DOWN_MASK != 0) keyModifierList += if(OperatingSystemChecker.isMac) "${UnicodeChars.MacAltKey}" else "Alt"
    if(modifiers and InputEvent.CTRL_DOWN_MASK != 0) keyModifierList += "Ctrl"
    if(modifiers and InputEvent.SHIFT_DOWN_MASK != 0) keyModifierList += "Shift"
    if(modifiers and InputEvent.META_DOWN_MASK != 0) keyModifierList += if(OperatingSystemChecker.isMac) "${UnicodeChars.MacCmdKey}" else "Meta"
    val keyText = getKeyText(keyCode)
    return if(keyModifierList.isEmpty()) keyText else keyModifierList.joinToString("+") + '+' + keyText
}


private fun getKeyText(keyCode:Int):String {
    return when {
        keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 || (keyCode >= KeyEvent.VK_A
                && keyCode <= KeyEvent.VK_Z) -> return keyCode.toChar().toString()
        keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9 -> "NUMPAD" + (keyCode - KeyEvent.VK_NUMPAD0 + '0'.toInt()).toChar()
        else -> return when (keyCode) {
            KeyEvent.VK_COMMA -> ","
            KeyEvent.VK_PERIOD -> "."
            KeyEvent.VK_SLASH -> "/"
            KeyEvent.VK_SEMICOLON -> ";"
            KeyEvent.VK_EQUALS -> "="
            KeyEvent.VK_OPEN_BRACKET -> "["
            KeyEvent.VK_BACK_SLASH -> "\\"
            KeyEvent.VK_CLOSE_BRACKET -> "]"
            KeyEvent.VK_ENTER -> "ENTER"
            KeyEvent.VK_BACK_SPACE -> "BACK_SPACE"
            KeyEvent.VK_TAB -> "TAB"
            KeyEvent.VK_SPACE -> "SPACE"
            KeyEvent.VK_LEFT -> "LEFT"
            KeyEvent.VK_RIGHT -> "RIGHT"
            KeyEvent.VK_UP -> "UP"
            KeyEvent.VK_DOWN -> "DOWN"
            KeyEvent.VK_ADD -> "+"
            KeyEvent.VK_PLUS -> "+"
            KeyEvent.VK_SUBTRACT -> "-"
            KeyEvent.VK_MINUS-> "-"
            KeyEvent.VK_MULTIPLY-> "*"
            KeyEvent.VK_DIVIDE-> "/"
            KeyEvent.VK_DELETE -> "Delete"
            KeyEvent.VK_ESCAPE -> "Escape"
            KeyEvent.VK_F1 -> "F1"
            KeyEvent.VK_F2 -> "F2"
            KeyEvent.VK_F3 -> "F3"
            KeyEvent.VK_F4 -> "F4"
            KeyEvent.VK_F5 -> "F5"
            KeyEvent.VK_F6 -> "F6"
            KeyEvent.VK_F7 -> "F7"
            KeyEvent.VK_F8 -> "F8"
            KeyEvent.VK_F9 -> "F9"
            KeyEvent.VK_F10 -> "F10"
            KeyEvent.VK_F11 -> "F11"
            KeyEvent.VK_F12 -> "F12"
            else -> "<UNKNOWN>"
        }
    }
}

fun JSplitPane.setDividerLocationAnimated(location:Int) {
    var timer:Timer? = null
    val targetLocation = min(max(1, location),if(orientation == JSplitPane.VERTICAL_SPLIT) height else width)
    val needsToGrow = dividerLocation < location
    //println("needsToGrow=$needsToGrow  dividerLocation=$dividerLocation  targetLocation=$targetLocation  location=$location")
    timer = Timer(10, ActionListener {
        if(dividerLocation == targetLocation) {
            //println("timer stop")
            timer?.stop()
        }
        if(needsToGrow) {
            if (dividerLocation + 10 <= targetLocation) {
                dividerLocation += 10
                //println("grow: updateLocation to $dividerLocation")
            } else {
                dividerLocation = targetLocation
            }
        }
        else {
            if (dividerLocation - 10 >= targetLocation) {
                dividerLocation -= 10
                //println("shrink: updateLocation to $dividerLocation")
            } else {
                dividerLocation = targetLocation
            }
        }
    })
    timer.start()
}
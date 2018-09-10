/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.view.input.TextEditorInputMode
import krayon.editor.base.command.ApplicationCommand
import krayon.editor.base.command.CommandManager
import krayon.editor.base.command.CommandScope
import krayon.editor.base.util.geim
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextArea
import javax.swing.KeyStroke

class UnicodeTextEditorInputMode : TextEditorInputMode() {

    private val commandShortcutListener:CommandShortcutKeyListener = CommandShortcutKeyListener()
    var encorcedCaretPos = -1

    init {
        textArea.addKeyListener(commandShortcutListener)
        textArea.addCaretListener { e ->
            if(encorcedCaretPos >= 0 && e.dot != encorcedCaretPos && textArea.document.length >= encorcedCaretPos) {
                textArea.caretPosition = encorcedCaretPos
                encorcedCaretPos = -1
            }
        }
    }

    abstract class CharConversionCommand(id:String):ApplicationCommand(id, CommandScope.ENTER_TEXT) {
        override fun canExecute(param: Any?): Boolean {
            return if(param is Char) return translateChar(param) != null else false
        }

        override fun execute(param: Any?) {
            (graphComponent.geim.mutexOwner as? UnicodeTextEditorInputMode)?.let { inputMode ->
                //println("execute param=$param")
                (param as? Char)?.let { char ->
                    translateChar(char)?.let { newChar ->
                        val caretPos = inputMode.textArea.caretPosition
                        val newText = inputMode.textArea.text.let { it.substring(0,caretPos) + newChar + it.substring(caretPos)}
                        inputMode.commandShortcutListener.convertedText = newText //inputMode.textArea.text + newChar
                        if(caretPos < inputMode.textArea.document.length) {
                            //insert not at end. since we replace the text, this will mess up caret pos.
                            //fix the mess via caretListener.
                            inputMode.encorcedCaretPos = caretPos + 1
                        }
                    }
                }
            }
        }
        abstract fun translateChar(c:Char):Char?
    }

    companion object {
        val ConvertToGreek = object:CharConversionCommand("CONVERT_TO_GREEK") {
            val greekMap = mapOf('a' to '\u03B1', 'b' to '\u03B2', 'c' to '\u03B3', 'd' to '\u03B4', 'e' to '\u03B5', 'f' to '\u03B6', 'g' to '\u03B7',
                    'A' to '\u0391', 'B' to '\u0392', 'C' to '\u0393', 'D' to '\u0394', 'E' to '\u0395', 'F' to '\u0396', 'G' to '\u0397')
            override fun translateChar(c: Char): Char? {
                return greekMap[c]
            }
        }

        val ConvertToSubscript = object:CharConversionCommand("CONVERT_TO_SUBSCRIPT") {
            val subscriptMap = mapOf('0' to '\u2080', '1' to '\u2081', '2' to '\u2082', '3' to '\u2083', '4' to '\u2084', '5' to '\u2085',
                    '6' to '\u2086', '7' to '\u2087', '8' to '\u2088', '9' to '\u2089', '+' to '\u208A', '-' to '\u208B',
                    '=' to '\u208C', '(' to '\u208D', ')' to '\u208E', 'm' to '\u2098', 'n' to '\u2099')



            override fun translateChar(c: Char): Char? {
                return subscriptMap[c]
            }
        }

        val ConvertToSuperscript = object:CharConversionCommand("CONVERT_TO_SUPERSCRIPT") {
            val superscriptMap = mapOf('0' to '\u2070', '1' to '\u00B9', '2' to '\u00B2', '3' to '\u00B3', '4' to '\u2074', '5' to '\u2075',
                    '6' to '\u2076', '7' to '\u2077', '8' to '\u2078', '9' to '\u2079', '+' to '\u207A', '-' to '\u207B',
                    '=' to '\u207C', '(' to '\u207D', ')' to '\u207E', 'n' to '\u207F')
            override fun translateChar(c: Char): Char? {
                return superscriptMap[c]
            }
        }
    }

    private class CommandShortcutKeyListener : KeyListener {

        var activeCommand:ApplicationCommand? = null
        var convertedText: String? = null

        override fun keyTyped(e: KeyEvent) {
            //println("keyTyped e=${e.keyChar}  activeCommand=${activeCommand?.id}")
            if(activeCommand != null) {
                CommandManager.execute(activeCommand!!,e.keyChar,CommandManager.InvocationMethod.VIA_KEYBOARD)
            }

            if(!e.isControlDown && !e.isMetaDown && !e.isAltDown)  {
                activeCommand = null
            }
        }

        override fun keyPressed(e: KeyEvent) {
            val keyStroke = KeyStroke.getKeyStroke(e.keyCode, e.modifiers)
            if(activeCommand == null) {
                activeCommand = CommandManager.getCommand(keyStroke, CommandScope.ENTER_TEXT)
                //println("keyPressed activeCommand = ${activeCommand?.id}")
            }
        }

        override fun keyReleased(e: KeyEvent) {
            //println("keyReleased: convertextText=$convertedText")
            if(convertedText != null) {
                val source = e.source as JTextArea
                source.text = convertedText
                convertedText = null
            }
        }
    }

}

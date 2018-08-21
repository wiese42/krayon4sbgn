/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import java.awt.*
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField

object ProblemReporter {

    fun reportThrowable(t: Throwable, message:String? = null, parentComponent:Component? = null) {
        //val details = ex.stackTrace.joinToString(separator = "\n") { it.toString() }
        val details = StringWriter().let { t.printStackTrace(PrintWriter(it)); it.buffer.toString() }
        val detailsPane = JScrollPane(JTextArea().apply {
            text = details
            font = Font("Lucida Sans Typewriter",Font.PLAIN,12)
        }).apply { preferredSize = Dimension(800,400) }
        val messageArray = arrayOf(
                "Problem", JTextField().apply {
                    text = message ?: t.message
                },
                "Details", detailsPane
        )
        JOptionPane.showMessageDialog(parentComponent, messageArray, "A problem has occurred.", JOptionPane.ERROR_MESSAGE)
    }

    fun installForUncaughtSwingExceptions() {
        Toolkit.getDefaultToolkit().systemEventQueue.push(EventQueueProxy())
    }

    internal class EventQueueProxy : EventQueue() {
        override fun dispatchEvent(newEvent: AWTEvent) {
            try {
                super.dispatchEvent(newEvent)
            } catch (t: Throwable) {
                t.printStackTrace()
                ProblemReporter.reportThrowable(t,null, null)
            }
        }
    }
}
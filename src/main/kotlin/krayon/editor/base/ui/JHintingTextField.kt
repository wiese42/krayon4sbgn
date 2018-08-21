/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

/* (@)JHintingTextField.java
Copyright 2009 Sebastian Haufe

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.awt.Color
import java.awt.Graphics
import java.awt.Shape
import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.text.BadLocationException
import javax.swing.text.Document
import javax.swing.text.Highlighter
import javax.swing.text.Highlighter.HighlightPainter
import javax.swing.text.JTextComponent


class JHintingTextField : JTextField {

    /**
     * Creates a new `JHintingTextField`.
     */
    constructor() {
        installHighlightPainter()
    }

    @Suppress("unused")
            /**
     * Creates a new `JHintingTextField`.
     *
     * @param columns the number of preferred columns to calculate preferred
     * width
     */
    constructor(columns: Int) : super(columns) {
        installHighlightPainter()
    }

    @Suppress("unused")
            /**
     * Creates a new `JHintingTextField`.
     *
     * @param text the text to show in the text field
     */
    constructor(text: String) : super(text) {
        installHighlightPainter()
    }

    @Suppress("unused")
            /**
     * Creates a new `JHintingTextField`.
     *
     * @param text the text to show in the text field
     * @param columns the number of preferred columns to calculate preferred
     * width
     */
    constructor(text: String, columns: Int) : super(text, columns) {
        installHighlightPainter()
    }

    @Suppress("unused")
            /**
     * Creates a new `JHintingTextField`.
     *
     * @param doc the text model
     * @param text the text to show in the text field
     * @param columns the number of preferred columns to calculate preferred
     * width
     */
    constructor(doc: Document, text: String, columns: Int) : super(doc, text, columns) {
        installHighlightPainter()
    }

    // -------------------------------------------------------------------------
    // Hinting highlighter code
    // -------------------------------------------------------------------------
    private fun installHighlightPainter() {
        val highlighter = highlighter
        //println(highlighter)
        try {
            highlighter.addHighlight(0, 0, createHighlightPainter())
        } catch (ex: BadLocationException) {
            assert(false) { "0:0 illegal?" } //$NON-NLS-1$
        }

    }

    private fun createHighlightPainter(): HighlightPainter {
        return object : Highlighter.HighlightPainter {

            val label = JLabel("", SwingConstants.TRAILING)
            val gap = 3

            override fun paint(g: Graphics, p0: Int, p1: Int, bounds: Shape, c: JTextComponent) {
                val hint = c.getClientProperty("emptyTextHint") as? String
                if (hint == null || hint.isEmpty() || c.document.length != 0)  return
                label.text = hint
                val ins = c.insets
                val ltr = c.componentOrientation.isLeftToRight
                if (ltr) {
                    ins.right += gap
                } else {
                    ins.left += gap
                }

                val pref = label.preferredSize
                val prHeight = pref.height
                val prWidth = pref.width
                val w = Math.min(c.width - ins.left - ins.right, prWidth)
                val h = Math.min(c.width - ins.top - ins.bottom, prHeight)
                val x = if (!ltr) c.width - ins.right - w else ins.left
                val parentHeight = c.height - ins.top - ins.bottom
                val y = ins.top + (parentHeight - h) / 2
                label.foreground = Color.GRAY
                label.isOpaque = false
                SwingUtilities.paintComponent(g, label, c, x, y, w, h)
            }
        }
    }
}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JComboBox

class NavigateComboBoxItems(val comboBox: JComboBox<*>, val next: Boolean) : AbstractAction(if (next) "Next" else "Previous") {

    init {
        tooltip = if (next) "Go to next item" else "Go to previous item"
    }

    override fun actionPerformed(e: ActionEvent) {
        val newIndex = if(next) {
            (comboBox.selectedIndex + 1) % comboBox.itemCount
        } else {
            if(comboBox.selectedIndex == 0) comboBox.itemCount - 1 else comboBox.selectedIndex - 1
        }

        comboBox.selectedIndex = newIndex
    }
}
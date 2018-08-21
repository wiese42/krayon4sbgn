/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import krayon.editor.base.util.ApplicationSettings
import java.awt.event.ActionEvent
import javax.swing.*

object UiFactory {
    fun createApplicationFrame(title:String):JFrame {
        return JFrame(title).apply {
            iconImage = ImageIcon(ApplicationSettings.APPLICATION_ICON.asResource()).image
        }
    }

    fun createAction(code:(ActionEvent) -> Unit): Action {
        return object: AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                code(e)
            }
        }
    }

    fun createToggleButton(action: Action): JToggleButton {
        return JToggleButton(action).apply {
            action.selectedIcon?.let { selectedIcon = it }
            action.icon?.let { icon = it }
        }
    }

    fun createStateButton(action: Action): JButton {
        return StateButton(action)
    }

    class StateButton(action:Action):JButton() {
        init {
            configurePropertiesFromAction(action)
            isSelected = action.isSelected == true
            icon = if(isSelected) action.selectedIcon ?: action.icon else action.icon
            addActionListener {
                isSelected = !isSelected
                action.isSelected = isSelected
                icon = if(isSelected) action.selectedIcon ?: action.icon else action.icon
                action.actionPerformed(it)
            }
            action.addPropertyChangeListener {
                if(it.propertyName == "SwingSelectedKey") {
                    isSelected = it.newValue == true
                    icon = if(isSelected) action.selectedIcon ?: action.icon else action.icon
                }
            }
        }
    }

}
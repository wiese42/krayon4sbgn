/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import krayon.editor.base.Application
import java.awt.GraphicsEnvironment

object ToggleFullScreenMode : ApplicationCommand("TOGGLE_FULLSCREEN_MODE") {
    override fun execute(param: Any?) {
        Application.applicationFrame?.let { frame ->
            val device = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
            frame.dispose()
            if(frame.isUndecorated) {
                device.fullScreenWindow = null
                frame.isUndecorated = false
            }
            else {
                frame.isUndecorated = true
                device.fullScreenWindow = frame
            }
            frame.isVisible = true
            frame.repaint()
        }
    }

    override fun canExecute(param: Any?): Boolean {
        return true
    }

}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import krayon.editor.base.command.ApplicationCommand
import krayon.editor.base.ui.ExtensionFileFilter
import krayon.editor.base.ui.showSaveDialogFX
import krayon.editor.base.util.ApplicationSettings
import krayon.editor.sbgn.io.SbgnWriter
import java.io.File
import java.io.FileOutputStream
import javax.swing.JFileChooser

class SaveSbgnCommand(id:String, private val includeStyle: Boolean = true) : SbgnCommand(id) {

    private val fileChooser:JFileChooser by lazy {
        JFileChooser().apply {
            isAcceptAllFileFilterUsed = false
            addChoosableFileFilter(ExtensionFileFilter("SBGN format (*.sbgn, *.xml)", "sbgn", "xml"))
        }
    }

    override fun execute(param: Any?) {
        fileChooser.apply {
            dialogTitle = "Save as SBGN File"
            dialogType = JFileChooser.SAVE_DIALOG
            (ApplicationSettings.DIAGRAM_FILE.scoped(graphComponent).value as? File)?.let { selectedFile = it }
        }

        val fileName = param as? String ?:
        if(fileChooser.showSaveDialogFX(graphComponent) == JFileChooser.APPROVE_OPTION) {
            ApplicationSettings.DIAGRAM_FILE.scoped(graphComponent).value = fileChooser.selectedFile
            fileChooser.selectedFile.absolutePath
        }
        else return
        SbgnWriter(includeStyle).write(FileOutputStream(fileName), graph, sbgnGraphComponent)
    }

    override fun canExecute(param: Any?) = true
}

val SaveSbgn:ApplicationCommand = SaveSbgnCommand("SAVE_SBGN")
val SavePlainSbgn:ApplicationCommand = SaveSbgnCommand("SAVE_PLAIN_SBGN", false)

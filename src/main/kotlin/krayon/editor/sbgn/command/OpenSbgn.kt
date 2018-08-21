/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import com.yworks.yfiles.view.input.ICommand
import krayon.editor.base.Application
import krayon.editor.base.ApplicationEvent
import krayon.editor.base.ui.ExtensionFileFilter
import krayon.editor.base.ui.showOpenDialogFX
import krayon.editor.base.util.ApplicationSettings
import krayon.editor.sbgn.io.SbgnReader
import java.io.File
import java.io.FileInputStream
import javax.swing.JFileChooser

val OpenSbgn = object:SbgnCommand("OPEN_SBGN") {

    private val fileChooser:JFileChooser by lazy {
        JFileChooser().apply {
            isAcceptAllFileFilterUsed = false
            addChoosableFileFilter(ExtensionFileFilter("SBGN format (*.sbgn, *.xml)", "sbgn", "xml"))
        }
    }

    override fun execute(param: Any?) {
        var file = param as? File
        if(file == null) {
            fileChooser.apply {
                dialogTitle = "Open SBGN File"
                dialogType = JFileChooser.OPEN_DIALOG
                (ApplicationSettings.LAST_FILE_LOCATION.value as? String)?.let { currentDirectory = File(it) }
            }
            var fileName = if (fileChooser.showOpenDialogFX(graphComponent) == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile.absolutePath
            } else return
            if (fileChooser.choosableFileFilters.none { it.accept(File(fileName)) }) fileName += ".sbgn"
            file = File(fileName)
        }

        graph.clear()
        ApplicationSettings.LAST_FILE_LOCATION.value = file.parent

        SbgnReader().read(FileInputStream(file), graph, sbgnGraphComponent)
        ApplicationSettings.DIAGRAM_FILE.scoped(graphComponent).value = file

        Application.fireApplicationEvent(ApplicationEvent(this,"DIAGRAM.LOADED"))

        ICommand.FIT_GRAPH_BOUNDS.execute(null, graphComponent)
    }

    override fun canExecute(param: Any?):Boolean {
        return true
    }
}

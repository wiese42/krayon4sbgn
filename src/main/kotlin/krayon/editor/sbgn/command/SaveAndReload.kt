/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.command

import krayon.editor.sbgn.io.SbgnReader
import krayon.editor.sbgn.io.SbgnWriter
import krayon.editor.sbgn.ui.SbgnGraphComponent
import java.io.FileInputStream
import java.io.FileOutputStream

object SaveAndReload : SbgnCommand("SAVE_AND_RELOAD") {
    override fun canExecute(param: Any?) = true
    override fun execute(param: Any?) {
        val file = "tmp.sbgn"
        val graph = graph
        FileOutputStream(file).use {
            SbgnWriter().write(it, graph, sbgnGraphComponent)
        }
        graph.clear()
        FileInputStream(file).use{
            SbgnReader().read(it, graph, sbgnGraphComponent)
        }
        println("saved and reloaded $file")
        graph.invalidateDisplays()
    }
}
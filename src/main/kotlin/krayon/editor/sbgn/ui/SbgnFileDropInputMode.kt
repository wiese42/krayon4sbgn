/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.ui

import com.yworks.yfiles.graph.DefaultGraph
import com.yworks.yfiles.graph.GraphCopier
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.view.input.DropInputMode
import krayon.editor.base.Application
import krayon.editor.base.ApplicationEvent
import krayon.editor.base.util.getBounds
import krayon.editor.base.util.minus
import krayon.editor.sbgn.KrayonForSbgn.graphComponent
import krayon.editor.sbgn.command.OpenSbgn
import krayon.editor.sbgn.io.SbgnReader
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTargetDragEvent
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.swing.TransferHandler

class SbgnFileDropInputMode : DropInputMode(DataFlavor.javaFileListFlavor){

    init {
        addDragDroppedListener { _, _ ->
            // drop data is list of dropped files
            @Suppress("UNCHECKED_CAST")
            val droppedFiles = dropData as List<File>
            // take the first
            val file = droppedFiles[0]
            try {
                if(dropAction == TransferHandler.MOVE) {
                    OpenSbgn.execute(file)
                }
                else if(dropAction == TransferHandler.COPY) {
                    pasteGraph(file)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun pasteGraph(file:File) {
        FileInputStream(file).use { stream ->
            val newGraph = DefaultGraph()
            SbgnReader().read(stream, newGraph, null)
            val offset = mousePosition - newGraph.getBounds().center
            val selection = graphComponent.selection
            selection.clear()
            GraphCopier().copy(newGraph, { _ -> true }, graphComponent.graph, offset, { _, copiedItem ->
                if (copiedItem is INode) {
                    selection.setSelected(copiedItem, true)
                }
            })

            Application.fireApplicationEvent(ApplicationEvent(this,"DIAGRAM.LOADED"))
        }
    }

    override fun acceptDrag(e: DropTargetDragEvent): Boolean {

        if (super.acceptDrag(e)) {
            dropAction = e.dropAction
            @Suppress("UNCHECKED_CAST")
            val droppedFiles = dropData as List<File>
            if (droppedFiles.size == 1) {
                // take the first
                val file = droppedFiles[0]
                // only accept sbgn files.
                if (file.extension == "sbgn" || file.extension == "xml") {
                    return true
                }
            }
        }
        return false
    }
}
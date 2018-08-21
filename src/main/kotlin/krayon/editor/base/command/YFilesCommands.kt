/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import com.yworks.yfiles.view.input.ICommand

object YFilesCommands {

    class YFilesCommand(private val iCommand: ICommand, id:String = iCommand.name, private val fixedParam:Any? = null) : ApplicationCommand(id) {
        override fun canExecute(param: Any?): Boolean {
            return iCommand.canExecute(fixedParam ?: param, graphComponent)
        }

        override fun execute(param: Any?) {
            iCommand.execute(fixedParam ?: param, graphComponent)
        }
    }

    val Copy = YFilesCommand(ICommand.COPY,"COPY")
    val Cut = YFilesCommand(ICommand.CUT,"CUT")
    val FitGraphBounds = YFilesCommand(ICommand.FIT_GRAPH_BOUNDS, "FIT_GRAPH_BOUNDS")
    val ZoomIn = YFilesCommand(ICommand.INCREASE_ZOOM,"ZOOM_IN")
    val ZoomOut = YFilesCommand(ICommand.DECREASE_ZOOM,"ZOOM_OUT")
    val ZoomToNormal = YFilesCommand(ICommand.ZOOM, "ZOOM_TO_NORMAL", 1.0)
    val Undo = YFilesCommand(ICommand.UNDO, "UNDO")
    val Redo = YFilesCommand(ICommand.REDO, "REDO")
    val SelectLeft = YFilesCommand(ICommand.MOVE_LEFT, "SELECT_LEFT")
    val SelectRight = YFilesCommand(ICommand.MOVE_RIGHT, "SELECT_RIGHT")
    val SelectAbove = YFilesCommand(ICommand.MOVE_UP, "SELECT_ABOVE")
    val SelectBelow = YFilesCommand(ICommand.MOVE_DOWN, "SELECT_BELOW")


}

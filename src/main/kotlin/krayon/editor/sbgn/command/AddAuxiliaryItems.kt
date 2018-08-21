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
import krayon.editor.sbgn.model.SbgnConstraintManager
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.type
import krayon.editor.sbgn.style.SbgnBuilder

open class AddAuxiliaryItem(name:String, val type:SbgnType) : ApplicationCommand(name)
{
    override fun canExecute(param: Any?): Boolean {
        return selectedNodes.size() == 1 && SbgnConstraintManager.isNodeAcceptingLabel(getUniqueNode(param)!!, type)
    }

    override fun execute(param: Any?) {
        getUniqueNode(param)?.let {
            val label = graph.addLabel(it,"")
            label.type = type
            SbgnBuilder.configure(graph, label)
            graphComponent.selection.clear()
            graphComponent.selection.setSelected(label, true)
        }
    }
}

object AddStateVariable : AddAuxiliaryItem("ADD_STATE_VARIABLE", SbgnType.STATE_VARIABLE)
object AddUnitOfInformation : AddAuxiliaryItem("ADD_UNIT_OF_INFORMATION", SbgnType.UNIT_OF_INFORMATION)

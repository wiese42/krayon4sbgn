/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import com.yworks.yfiles.graph.*
import com.yworks.yfiles.view.ISelectionModel
import com.yworks.yfiles.view.input.ICommand
import krayon.editor.base.Application
import krayon.editor.base.ui.*
import krayon.editor.base.util.IconManager
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.Icon
import javax.swing.KeyStroke

abstract class ApplicationCommand(final override val id: String, final override val scope: CommandScope = CommandScope.DEFAULT) : IApplicationCommand {

    var name:String? = null
    var keyStroke:KeyStroke? = null
    override var description:String? = null
    override var mouseGestureDescription: String? = null

    private val parameterizedActions = mutableListOf<ParameterizedAction>()
    val commandHandle = ICommand.createCommand(id)!!

    val graphComponent get() = Application.focusedGraphComponent!!
    val graph: IGraph get() = graphComponent.graph
    val selectedNodes: ISelectionModel<INode> get() = graphComponent.selection.selectedNodes
    val selectedEdges: ISelectionModel<IEdge> get() = graphComponent.selection.selectedEdges

    fun getNodes(param:Any?) = if(param is INode) listOf(param) else selectedNodes
    fun getUniqueNode(param:Any?) = param as? INode ?: with(graphComponent.selection.selectedNodes) { if(size() == 1) first() else null }
    fun getUniqueEdge(param:Any?) = param as? IEdge ?: with(graphComponent.selection.selectedEdges) { if(size() == 1) first() else null }
    fun getUniqueLabel(param:Any?) = param as? ILabel ?: with(graphComponent.selection.selectedLabels) { if(size() == 1) first() else null }
    fun getUniquePort(param:Any?) = param as? IPort ?: with(graphComponent.selection.selectedPorts) { if(size() == 1) first() else null }

    fun managedCanExecute(param:Any?):Boolean {
        if(Application.focusedGraphComponent == null) return false
        val result = canExecute(param)
        parameterizedActions.forEach {
            if (it.param == param) it.action.isEnabled = result
        }
        return result
    }

    internal data class ParameterizedAction(val action: Action, val param:Any? = null, val target:Any)

    fun getAction(target:Any, param:Any? = null): Action {
        val action = parameterizedActions.firstOrNull { it.param == param && it.target == target }?.action ?: createAction(target, param)
        action.isEnabled = managedCanExecute(param)
        return action
    }

    private fun createAction(target: Any, param:Any? = null): Action {
        val action = object: AbstractAction(name ?: id) {
            override fun actionPerformed(event: ActionEvent) {
                val parameter = getValue(Action.SELECTED_KEY)?.let { it } ?: param
                if(canExecute(parameter)) CommandManager.execute(this@ApplicationCommand, parameter, CommandManager.InvocationMethod.VIA_ACTION)
            }
        }
        parameterizedActions.add(ParameterizedAction(action, param, target))
        return action.apply {
            shortcut = keyStroke
            tooltip = convertToHtmlTooltip(description ?: name ?: id) + ' ' + action.shortcutString
            icon = icon32
            selectedIcon = selectedIcon32
        }
    }

    private fun convertToHtmlTooltip(text:String, lineBreakLimit:Int=40):String {
        val words = text.split(' ')
        var lineLength = 0
        val result = StringBuffer("<html>")
        words.forEach { word ->
            if(lineLength > 0 && lineLength + word.length > lineBreakLimit) {
                result.append("<br>$word")
                lineLength = word.length
            }
            else {
                result.append(" $word")
                lineLength += word.length + 1
            }
        }
        return result.toString()
    }

    protected fun updateActionSelectionState(isSelected:Boolean) {
        parameterizedActions.forEach {
            it.action.isSelected = isSelected
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    val icon32: Icon? get() = IconManager.iconMap["${scope.name}.$id"]?.icon32
    @Suppress("MemberVisibilityCanBePrivate")
    val selectedIcon32: Icon? get() = IconManager.iconMap["${scope.name}.$id"]?.selectedIcon32
    val icon16: Icon? get() = IconManager.iconMap["${scope.name}.$id"]?.icon16
    val selectedIcon16: Icon? get() = IconManager.iconMap["${scope.name}.$id"]?.selectedIcon16

}
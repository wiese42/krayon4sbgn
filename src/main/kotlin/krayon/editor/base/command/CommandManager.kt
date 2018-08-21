/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import com.beust.klaxon.JsonReader
import com.yworks.yfiles.view.input.CreateEdgeInputMode
import com.yworks.yfiles.view.input.KeyboardInputMode
import krayon.editor.base.Application
import krayon.editor.base.ui.GraphPaletteDropInputMode
import krayon.editor.base.ui.UnicodeTextEditorInputMode
import krayon.editor.base.util.ceim
import krayon.util.OperatingSystemChecker
import java.io.InputStream
import javax.swing.KeyStroke

//private typealias Context = String?
private typealias Id = String

object CommandManager {

    enum class InvocationMethod {
        VIA_KEYBOARD,
        VIA_ACTION,
        UNSPECIFIED
    }

    private val scopeCommandMap = mutableMapOf<CommandScope, MutableMap<Id, ApplicationCommand>>()

    fun initializeKeyMap(stream: InputStream) {
        JsonReader(stream.reader()).use { reader ->
            reader.beginArray {
                while (reader.hasNext()) {
                    reader.nextObject().let { item ->
                        item.string("id")?.let {
                            getScopeAndCommand(it)?.let { (scope, id) ->
                                getCommand(id, scope)?.let { command ->
                                    item.string("key-stroke")?.let {
                                        val keyString = if (OperatingSystemChecker.isMac) it.replace("ctrl", "meta") else it
                                        KeyStroke.getKeyStroke(keyString)?.let { keyStroke ->
                                            command.keyStroke = keyStroke
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if(System.getProperty("verbose") != null) {
            scopeCommandMap.forEach { context, commandMap ->
                commandMap.values.forEach { command ->
                    //println("scopeCommandMap: ${command.id}  context=$context")
                    if(command.keyStroke == null)  {
                        val id = if(context == CommandScope.DEFAULT) command.id else "$context/${command.id}"
                        println("command $id not bound to any keystroke")
                    }
                }
            }
        }
    }

    private fun getScopeAndCommand(dotNotation:String):Pair<CommandScope, String>? {
        return dotNotation.split('.').let { list ->
            when(list.size) {
                0 -> null
                1 -> Pair(CommandScope.DEFAULT,list[0])
                else -> Pair(getCommandScopeByName(list[0]), list[1])
            }
        }
    }

    private fun getCommandScopeByName(name:String?):CommandScope {
        return if(name == null) CommandScope.DEFAULT
        else CommandScope.values().first{ it.name == name}
    }

    fun getCommands():Iterable<ApplicationCommand> = scopeCommandMap.values.map{ it.values }.flatten()

    private fun getCommand(id:String, scope:CommandScope = CommandScope.DEFAULT):ApplicationCommand? {
        return scopeCommandMap[scope]?.get(id)
    }

    fun initializeTextualResources(stream: InputStream) {
        JsonReader(stream.reader()).use { reader ->
            reader.beginArray {
                while (reader.hasNext()) {
                    reader.nextObject().let { item ->
                        item.string("id")?.let {
                            getScopeAndCommand(it)?.let { (scope, id) ->
                                getCommand(id, scope)?.let { command ->
                                    item.string("name")?.let { name ->
                                        command.name = name
                                    }
                                    item.string("description")?.let { description ->
                                        command.description = description
                                    }
                                    item.string("mouse_gesture")?.let { gesture ->
                                        command.mouseGestureDescription = gesture
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        if (System.getProperty("verbose") != null) {
            scopeCommandMap.forEach { context, commandMap ->
                commandMap.values.forEach { command ->
                    if (command.name == null) {
                        val id = if (context == CommandScope.DEFAULT) command.id else "$context/${command.id}"
                        println("command $id has no name")
                    }
                }
            }
        }
    }

    private fun hasValidCommandContext(command:ApplicationCommand):Boolean {
        return when(Application.focusedGraphComponent?.ceim?.mutexOwner) {
            null -> command.scope == CommandScope.DEFAULT
            is CreateEdgeInputMode -> command.scope == CommandScope.CREATE_EDGE
            is GraphPaletteDropInputMode -> command.scope == CommandScope.DRAG_ITEM
            is UnicodeTextEditorInputMode -> command.scope == CommandScope.ENTER_TEXT
            else -> false
        }
    }

    fun execute(command:ApplicationCommand, param:Any? = null, invocationMethod:InvocationMethod = InvocationMethod.UNSPECIFIED ) {
        //println("execute: " + command.id + "  param=" + param)
        if(hasValidCommandContext(command) && command.canExecute(param)) {
            fireCommandEvent(command, param, invocationMethod)
            command.execute(param)

        }
    }

    fun add(command: ApplicationCommand) {
        var commandMap = scopeCommandMap[command.scope]
        if(commandMap == null) {
            commandMap = mutableMapOf()
            scopeCommandMap[command.scope] = commandMap
        }
        commandMap[command.id] = command
    }

    operator fun plusAssign(command: ApplicationCommand) {
        add(command)
    }

    fun getCommand(keyStroke:KeyStroke, scope: CommandScope = CommandScope.DEFAULT):ApplicationCommand? {
        return scopeCommandMap[scope]!!.values.firstOrNull { it.keyStroke == keyStroke }
    }

    fun registerKeyBindings(kim: KeyboardInputMode) {
        //println("registerKeyBindings")
        scopeCommandMap[CommandScope.DEFAULT]?.values?.forEach { command ->
            if(command.keyStroke != null) {
                //println("command=${command.id}  keyStroke=${command.keyStroke?.toShortcutString()}")
                kim.apply {
                    addCommandBinding(command.commandHandle,
                            { iCommand, param, _ ->
                                getCommand(iCommand.name)?.let { execute(it,param, InvocationMethod.VIA_KEYBOARD) }
                                //getCommand(iCommand.name)?.execute(param)
                                true
                            },
                            { iCommand, param, target ->
                                val result = if(target != null) getCommand(iCommand.name)?.managedCanExecute(param) ?: false else false
                                result
                            })
                    addKeyBinding(command.keyStroke, command.commandHandle)
                }
            }

        }
    }

    class ExecutionEvent(val command: ApplicationCommand, val param:Any?, val invocationMethod: InvocationMethod)
    interface CommandListener {
        fun onExecute(event:ExecutionEvent)
    }
    val commandListeners = mutableListOf<CommandListener>()
    private fun fireCommandEvent(command: ApplicationCommand, param: Any?, invocationMethod: InvocationMethod) {
        if(commandListeners.any()) {
            val event = ExecutionEvent(command, param, invocationMethod)
            commandListeners.forEach { it.onExecute(event) }
        }
    }
}
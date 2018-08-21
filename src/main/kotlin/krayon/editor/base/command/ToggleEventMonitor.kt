/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import krayon.editor.base.ui.toShortcutString
import krayon.editor.sbgn.command.SbgnCommand
import krayon.util.EventMonitor

object ToggleEventMonitor  : SbgnCommand("TOGGLE_EVENT_MONITOR") {

    private var eventMonitor: EventMonitor? = null

    override fun canExecute(param: Any?) = true

    override fun execute(param: Any?) {
        println("toggle event monitor")
        if (eventMonitor == null) eventMonitor = createEventMonitor()
        if(eventMonitor!!.isRunning) eventMonitor!!.stop() else eventMonitor!!.start()
    }

    private fun createEventMonitor(): EventMonitor {
        val eventSource = object: EventMonitor.IEventSource {
            override fun addCallback(callback: (EventMonitor.EventDescription) -> Unit) {
                CommandManager.commandListeners += object:CommandManager.CommandListener {
                    override fun onExecute(event: CommandManager.ExecutionEvent) {
                        if(event.invocationMethod != CommandManager.InvocationMethod.UNSPECIFIED) {
                            if(event.invocationMethod == CommandManager.InvocationMethod.VIA_KEYBOARD && event.command.keyStroke != null) {
                                val shortCut = event.command.keyStroke!!.toShortcutString()
                                callback.invoke(EventMonitor.EventDescription("[ $shortCut ]  ${createCommandString(event.command, event.param)}"))
                            }
                            else {
                                callback.invoke(EventMonitor.EventDescription(createCommandString(event.command, event.param)))
                            }
                        }
                    }
                }
            }
        }

        return EventMonitor(graphComponent, eventSource).apply {
            textColor = graphComponent.background
            background = sbgnGraphComponent.highlightPen.paint
            pen = null
        }
    }

    private fun createCommandString(command:ApplicationCommand, param:Any?):String {
        val booleanParam = param as? Boolean
        val name = command.name ?: command.id
        return if(booleanParam == null) name  else "$name = $booleanParam"
    }
}

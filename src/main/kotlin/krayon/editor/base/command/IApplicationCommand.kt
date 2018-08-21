/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

interface IApplicationCommand {
    val id:String
    val scope: CommandScope
    val description:String?
    val mouseGestureDescription:String?
    fun canExecute(param:Any? = null):Boolean
    fun execute(param:Any? = null)
}

enum class CommandScope {
    DEFAULT,
    CREATE_EDGE,
    DRAG_ITEM,
    ENTER_TEXT
}




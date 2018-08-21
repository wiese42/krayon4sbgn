/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.util

import com.beust.klaxon.JsonReader
import krayon.util.ResourceLoader
import java.io.InputStream
import javax.swing.Icon
import javax.swing.ImageIcon

object IconManager {

    class Icons(val icon16: Icon?, val icon32:Icon?, val selectedIcon16:Icon? = null, val selectedIcon32:Icon? = null)

    val iconMap = mutableMapOf<String,Icons>()
    var iconMapPath: String? = null
    var iconPath16: String? = null
    var iconPath32: String? = null

    fun initializeIconResources(stream: InputStream) {
        JsonReader(stream.reader()).use { reader ->
            reader.beginArray {
                while (reader.hasNext()) {
                    reader.nextObject().let { item ->
                        item.string("id")?.let { id ->
                            val key = if(id.contains('.')) id else "DEFAULT.$id"
                            iconMap[key] = loadIcons(item.string("icon"), item.string("selected-icon"))
                        }
                    }
                }
            }
        }
    }

    private fun loadIcons(iconBaseName: String?, selectedIconBaseName: String?): Icons {
        val icon16 = loadIcon("$iconPath16/$iconBaseName.png")
        val icon32 = loadIcon("$iconPath32/$iconBaseName.png")
        val selectedIcon16 = if(selectedIconBaseName == null) null else
            loadIcon("$iconPath16/$selectedIconBaseName.png")
        val selectedIcon32 = if(selectedIconBaseName == null) null else
            loadIcon("$iconPath32/$selectedIconBaseName.png")
        return Icons(icon16, icon32, selectedIcon16, selectedIcon32)
    }

    private fun loadIcon(iconPath:String):Icon? {
        return try {
            ResourceLoader.getResource(iconPath)?.let {
                ImageIcon(it)
            }
        }
        catch(ex:Exception) {
            println("cannot load icon from $iconPath")
            null
        }
    }


}
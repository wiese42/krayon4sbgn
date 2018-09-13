/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.util

import java.awt.Dimension
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.util.*

data class PropertyKey(val name:String, val isPersistant:Boolean = false, val scope:Any? = null) {

    fun asResource():URL? {
        return javaClass.getResource(ApplicationSettings[this] as String)
    }
    var value:Any?
        set(v) {
            ApplicationSettings[this] = v
        }
        get() = ApplicationSettings[this]

    override fun toString() = name

    fun scoped(scope:Any) = PropertyKey(name, isPersistant, scope)

    var dimension: Dimension?
        set(v) {
            if(v != null) ApplicationSettings[this] = "[${v.width},${v.height}]"
            else ApplicationSettings[this] = null
        }
        get() {
            val list = ApplicationSettings[this]?.toString()?.split('[',',',']')
            return if(list != null) Dimension(list[1].toInt(), list[2].toInt()) else null
        }
}

object ApplicationSettings {
    var backingFile:File? = null

    private val pcs = PropertyChangeSupport(this)

    private val properties = mutableMapOf<PropertyKey, Any?>()

    val LAST_FILE_LOCATION = PropertyKey("LAST_FILE_LOCATION", true)
    val APPLICATION_ICON = PropertyKey("APPLICATION_ICON")
    val APPLICATION_TITLE = PropertyKey("APPLICATION_TITLE")
    val APPLICATION_WINDOW_SIZE = PropertyKey("APPLICATION_WINDOW_SIZE", true)
    val APPLICATION_VERSION = PropertyKey("APPLICATION_VERSION")
    val APPLICATION_RESOURCE_PATH = PropertyKey("APPLICATION_RESOURCE_PATH")
    val DIAGRAM_FILE = PropertyKey("DIAGRAM_FILE")
    val CANVAS_ICON_PATH = PropertyKey("CANVAS_ICON_PATH")
    val LOOK_AND_FEEL = PropertyKey("LOOK_AND_FEEL", true)
    val DEFAULT_SBGN_STYLE = PropertyKey("DEFAULT_SBGN_STYLE", true)
    val DEFAULT_HIGHLIGHT_COLOR = PropertyKey("DEFAULT_HIGHLIGHT_COLOR")

    fun addPropertyChangeListener(listener:PropertyChangeListener) = pcs.addPropertyChangeListener(listener)
    @Suppress("unused")
    fun removePropertyChangeListener(listener: PropertyChangeListener) = pcs.removePropertyChangeListener(listener)

    operator fun set(key:PropertyKey, value:Any?) {
        val oldValue = properties[key]
        if(oldValue != value) {
            properties[key] = value
            pcs.firePropertyChange(PropertyChangeEvent(key.scope ?: this,key.name, oldValue, value))
            if(key.isPersistant) save()
        }
    }

    operator fun get(key:PropertyKey):Any? {
        return properties[key]
    }

    private fun save() {
        if(backingFile != null) {
            if (backingFile?.exists() == false) {
                backingFile!!.parentFile.mkdirs()
            }
            val jProperties = Properties().apply { properties.forEach { if (it.key.isPersistant) set(it.key.name, it.value) } }
            FileOutputStream(backingFile).use {
                jProperties.storeToXML(it, "", "utf-8")
            }
        }
    }

    fun load() {
        if(backingFile?.exists() == true) {
            FileInputStream(backingFile).use { stream ->
                properties.clear()
                val jProperties = Properties()
                jProperties.loadFromXML(stream)
                jProperties.forEach {
                    val pKey = PropertyKey(it.key as String, true)
                    properties[pKey] = it.value as String
                }
            }
        }
    }
}


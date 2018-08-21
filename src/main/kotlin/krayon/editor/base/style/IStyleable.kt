/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.style

import com.yworks.yfiles.geometry.InsetsD
import com.yworks.yfiles.graph.IGraph
import com.yworks.yfiles.graph.IModelItem
import com.yworks.yfiles.view.GraphComponent
import java.awt.Color
import java.awt.Font
import java.awt.Paint

interface IStyleable {
    fun applyStyle(context:IStyleableContext, map:Map<StyleProperty,Any?>)
    fun retrieveStyle(context:IStyleableContext, map:MutableMap<StyleProperty,Any?>)
}

fun IStyleable.applyStyle(item: IModelItem, graph:IGraph, map:Map<StyleProperty, Any?>) = applyStyle(DefaultStyleableContext(item, graph), map)
fun IStyleable.applyStyle(graphComponent: GraphComponent, map:Map<StyleProperty, Any?>) = applyStyle(DefaultStyleableContext(graphComponent, graphComponent.graph, graphComponent), map)

fun IStyleable.retrieveStyle(item: IModelItem, graph:IGraph, map:MutableMap<StyleProperty, Any?>) = retrieveStyle(DefaultStyleableContext(item, graph), map)
fun IStyleable.retrieveStyle(graphComponent: GraphComponent, map:MutableMap<StyleProperty, Any?>) = retrieveStyle(DefaultStyleableContext(graphComponent, graphComponent.graph, graphComponent), map)

interface IStyleableContext {
    val graphComponent:GraphComponent?
    val graph: IGraph?
    val item:Any?
}

class DefaultStyleableContext(override val item:Any?, override val graph: IGraph? = null, override val graphComponent: GraphComponent? = null) : IStyleableContext

enum class StyleProperty(val valueType:Class<*>) {

    Background(Paint::class.java),
    BackgroundColor(Color::class.java),
    OutlineColor(Color::class.java),
    OutlineWidth(Double::class.java),
    Width(Double::class.java),
    Height(Double::class.java),

    FontSize(Double::class.java),
    FontStyle(FontStyleValue::class.java),
    FontFamily(String::class.java),
    TextColor(Color::class.java),
    LabelBackgroundColor(Color::class.java),
    LabelOutlineColor(Color::class.java),
    LabelOutlineWidth(Double::class.java),
    LabelInsets(InsetsD::class.java),

    MapBackgroundColor(Color::class.java),

    PathColor(Color::class.java),
    PathWidth(Double::class.java),
    PathCornerRadius(Double::class.java),
    ArrowScale(Double::class.java),

    StateVariableShape(StateVariableShapeValue::class.java),
    PortColor(Color::class.java),
    PortWidth(Double::class.java),
    DropShadow(Boolean::class.java),

    CloneMarkerBackground(Paint::class.java),
    CloneMarkerFontSize(Double::class.java),
    CloneMarkerFontStyle(FontStyleValue::class.java),
    CloneMarkerTextColor(Color::class.java),

    ShapeParameter(Double::class.java),

    HighlightColor(Color::class.java);

    val displayName get() = nameRegExp.replace(name) { " ${it.value}"}

    companion object {
        private val nameRegExp = """(?<!^)[A-Z]""".toRegex() //upperCase not at beginning
    }
}

enum class FontStyleValue(val code:Int) {
    Plain(Font.PLAIN),
    Bold(Font.BOLD),
    Italic(Font.ITALIC)
}

enum class StateVariableShapeValue {
    Ellipse,
    Capsule
}

class GraphStyle<T>(var name:String, val isReadOnly:Boolean = true, val styleTemplateMap: StyleTemplateMap<T>) {
    var isFileLocal = false

    //for debugging purposes
    fun compareStyle(other:GraphStyle<T>) {
        val otherName = other.name
        for ((typeKey,styleMap) in styleTemplateMap.entries) {
            val otherStyleMap:MutableMap<StyleProperty, Any?>? = other.styleTemplateMap[typeKey]
            if(otherStyleMap == null) {
                println("$name/$otherName otherStyleMap not there for $typeKey")
            }
            else {
                for ((key,value) in styleMap.entries) {
                    val otherValue = otherStyleMap[key]
                    if(otherValue == null) {
                        println("$name/$otherName -> other property $typeKey/$key not there.")
                    }
                    else if(value != otherValue) {
                        println("$name/$otherName $typeKey/$key values differ: $value <--> $otherValue")
                    }
                }
            }
        }
    }
}

typealias StyleAttributes = MutableMap<StyleProperty, Any?>
typealias StyleTemplateMap<T> = MutableMap<T, StyleAttributes>
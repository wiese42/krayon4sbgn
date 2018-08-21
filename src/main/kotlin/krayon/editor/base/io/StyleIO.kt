/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.io

import com.helger.css.handler.ICSSParseExceptionCallback
import com.helger.css.parser.ParseException
import com.helger.css.parser.Token
import com.helger.css.reader.CSSReader
import com.helger.css.reader.CSSReaderSettings
import com.helger.css.reader.errorhandler.ICSSParseErrorHandler
import com.helger.css.utils.CSSColorHelper
import com.helger.css.utils.CSSNumberHelper
import com.helger.css.writer.CSSWriterSettings
import com.yworks.yfiles.geometry.InsetsD
import com.yworks.yfiles.view.Pen
import krayon.editor.base.style.*
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.ui.ConfiguredSbgnPaletteComponent
import java.awt.Color
import java.awt.Paint
import java.io.Reader
import java.io.StringWriter
import java.io.Writer
import kotlin.math.roundToInt

open class StyleIO<T>  {

    var entityNameEncoder: (T) -> String? = { it.toString() }
    var classNameEncoder: (T) -> String? = { it.toString().toLowerCase().replace('_','-') }

    var classNameDecoder: (String) -> T? = { null }
    var entityNameDecoder: (String) -> List<T>? = { null }

    fun writeStyleMap(writer: Writer, graphStyle:GraphStyle<T>) {
        val buffer = StringBuilder()
        graphStyle.styleTemplateMap.forEach { type, styleMap ->
            val className = classNameEncoder(type)?.let { ".$it" } ?: ""
            val entityName = entityNameEncoder(type)?.let { it } ?: ""
            buffer.appendln("$entityName$className {")
            styleMap.keys.forEachIndexed { index, styleKey ->
                val value = styleMap[styleKey]
                buffer.append("  " + cssAttributeName(styleKey.name) + ": " + cssValue(value) + ";")
                if(index < styleMap.size-1) buffer.appendln()
            }
            buffer.appendln("}")
        }
        writer.append(buffer.toString())
    }

    fun cssAttributeName(string: String):String {
        return """[A-Z]""".toRegex().replace(string) { (if (it.range.first > 0) "-" else "") + it.value.toLowerCase() }
    }

    private fun decodeAttributeName(cssAttribute: String):StyleProperty? {
        return StyleProperty.values().find { cssAttributeName(it.name) == cssAttribute }
    }

    fun cssValue(any:Any?):String {
        return when(any) {
            is Double -> any.toString() + "px"
            is Color -> encodeColor(any)
            is OrientedGradientPaint -> encodeLinearGradient(any)
            is Enum<*> -> cssAttributeName(any.name)
            is Boolean -> any.toString()
            is InsetsD -> encodeInsets(any)
            else -> throw IllegalStateException("cannot convert ${any?.javaClass} to cssValue")
        }
    }

    private fun encodeInsets(insets:InsetsD):String {
        return "${insets.top} ${insets.right} ${insets.bottom} ${insets.left}"
    }

    private fun encodeLinearGradient(paint:OrientedGradientPaint):String {
        val args = (0 until paint.colors.size).joinToString(",", postfix = ")") { "${encodeColor(paint.colors[it])} ${(paint.fractions[it]*100.0).roundToInt()}%" }
        return "linear-gradient(${paint.degAngle.roundToInt()}deg, $args"
    }

    private fun encodeColor(color:Color):String {
        return if(color.alpha == 255) "rgb(${color.red},${color.green},${color.blue})" else "rgba(${color.red},${color.green},${color.blue}, ${color.alpha.toDouble()/255.0})"
    }

    private fun decodeAs(value:String?, clazz:Class<*>):Any? {
        if(value == null) return null
        return when (clazz) {
            Color::class.java -> decodeAsColor(value)
            Double::class.java -> {
                when {
                    CSSNumberHelper.isValueWithUnit(value) -> {
                        CSSNumberHelper.getValueWithUnit(value)?.value
                    }
                    CSSNumberHelper.isNumberValue(value) -> {
                        value.toDouble()
                    }
                    else -> null
                }
            }
            Paint::class.java -> {
                when {
                    isColor(value) -> decodeAsColor(value)
                    isLinearGradient(value) -> decodeAsLinearGradient(value)
                    else -> null
                }
            }
            Boolean::class.java -> value.toBoolean()
            InsetsD::class.java -> decodeAsInsets(value)
            String::class.java -> value
            FontStyleValue::class.java -> FontStyleValue.values().find { it.name.toLowerCase() == value }
            StateVariableShapeValue::class.java -> StateVariableShapeValue.values().find { it.name.toLowerCase() == value }
            else -> null
        }
    }

    private fun decodeAsInsets(value:String):InsetsD {
        val list = value.split(" ").map { it.toDouble() }
        val (top,right,bottom,left) = list
        return InsetsD.fromLTRB(left, top, right, bottom)
    }

    private fun isColor(value:String):Boolean {
        return CSSColorHelper.isRGBColorValue(value) || CSSColorHelper.isRGBAColorValue(value)
    }

    private fun decodeAsColor(value:String):Color {
        return when {
            CSSColorHelper.isRGBColorValue(value) -> {
                val rgb = CSSColorHelper.getParsedRGBColorValue(value)!!
                Color(rgb.red.toInt(), rgb.green.toInt(), rgb.blue.toInt())
            }
            CSSColorHelper.isRGBAColorValue(value) -> {
                val rgba = CSSColorHelper.getParsedRGBAColorValue(value)!!
                Color(rgba.red.toInt(), rgba.green.toInt(), rgba.blue.toInt(), (rgba.opacity.toDouble() * 255).toInt())
            }
            else -> throw IllegalArgumentException("unknown color value $value")
        }
    }

    private fun isLinearGradient(value:String):Boolean {
        return value.startsWith("linear-gradient(")
    }

    fun decodeAsLinearGradient(value:String):OrientedGradientPaint {
        val match = """linear-gradient\((\d+)deg,(.+)\)""".toRegex().matchEntire(value)
        if(match != null) {
            val degAngle = match.groupValues[1].toDouble()
            val colorList = match.groupValues[2]
            val findResult = """(rgb\(\d+,\d+,\d+\))\s(\d+)%|(rgba\(\d+,\d+,\d+,[\d.]+\))\s(\d+)%""".toRegex().findAll(colorList)
            val fractions = mutableListOf<Float>()
            val colors = mutableListOf<Color>()
            findResult.forEach { matchResult ->
                val index = if(matchResult.groupValues[1].isNotEmpty()) 1 else 3
                //println("color="+ matchResult.groupValues[index] + " fraction=" + matchResult.groupValues[index+1])
                colors.add(decodeAsColor(matchResult.groupValues[index]))
                fractions.add(matchResult.groupValues[index+1].toFloat()/100f)
            }
            return OrientedGradientPaint(degAngle, fractions.toFloatArray(), colors.toTypedArray())
        }
        throw IllegalArgumentException("Cannot parse linear-gradient $value")
    }

    fun readCss(reader: Reader):StyleTemplateMap<T> {

        val cssReaderSettings = CSSReaderSettings().apply {
            customErrorHandler = object:ICSSParseErrorHandler {
                override fun onCSSParseError(aParseEx: ParseException, aLastSkippedToken: Token?) {
                    throw  aParseEx
                }
                override fun onCSSUnexpectedRule(aCurrentToken: Token, sRule: String, sMsg: String) {
                    throw ParseException("CSS: Unexpected Rule $sRule")
                }

                override fun onCSSBrowserCompliantSkip(ex: ParseException?, aFromToken: Token, aToToken: Token) {
                    //ignore
                }
            }
            customExceptionHandler = ICSSParseExceptionCallback { ex -> throw ex }
        }

        val css = CSSReader.readFromReader({reader}, cssReaderSettings)
        val ws = CSSWriterSettings()

        val templateMap = mutableMapOf<T, MutableMap<StyleProperty, Any?>>()

        val types = mutableListOf<T>()
        css?.allStyleRules?.forEach { rule ->
            types.clear()
            rule.allSelectors.forEach { selector ->
                selector.allMembers.forEach { member ->
                    val memberName = member.getAsCSSString(ws)
                    if (memberName.startsWith('.')) {
                        val type = classNameDecoder(memberName.substring(1))
                        if (type != null) types.add(type)
                    }
                    if (memberName[0].isLetter()) {

                        val typeList = entityNameDecoder(memberName)
                        if (typeList != null) types.addAll(typeList)
                    }
                }

                if (types.isNotEmpty()) {
                    rule.allDeclarations.forEach { declaration ->
                        //println("property=" + declaration.property + " decoded=" + decodeAttributeName(declaration.property))
                        val styleProperty = decodeAttributeName(declaration.property)
                        if (styleProperty != null) {
                            val expression = declaration.expression.allMembers.joinToString(" ") { it.getAsCSSString(ws) }
                            val styleValue = decodeAs(expression, styleProperty.valueType)
                            if (styleValue != null) {
                                types.forEach { type ->
                                    if(!templateMap.containsKey(type)) {
                                        templateMap[type] = mutableMapOf()
                                    }
                                    val styleMap = templateMap[type]!!
                                    styleMap[styleProperty] = styleValue
                                }
                            }
                        }
                    }
                }
            }
        }
        return templateMap
    }
}
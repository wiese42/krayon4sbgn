/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.model

import com.yworks.yfiles.graph.*
import com.yworks.yfiles.utils.ICloneable
import krayon.editor.base.model.IItemType
import krayon.editor.base.style.GraphStyle

class SbgnData(var type: SbgnType = SbgnType.NO_TYPE, var property:HashMap<SbgnPropertyKey, Any?>? = null) : ICloneable, Cloneable, ILookup {

    var style:GraphStyle<SbgnType>? = null

    override fun <T : Any?> lookup(type: Class<T>?): T? {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            IMementoSupport::class.java -> SbgnInfoMementoSupport() as T
            else -> null
        }
    }

    override fun clone(): Any {
        val copy = super.clone() as SbgnData
        @Suppress("UNCHECKED_CAST")
        if(property != null) copy.property = property!!.clone() as HashMap<SbgnPropertyKey, Any?>
        return copy
    }

    override fun equals(other: Any?): Boolean {
        return other is SbgnData && type == other.type && SbgnPropertyKey.values().all { isPropertyEqual(it, other) }
    }

    private fun isPropertyEqual(key: SbgnPropertyKey, other: SbgnData):Boolean {
        return (property?.get(key) ?: false) == (other.property?.get(key) ?: false)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (property?.hashCode() ?: 0)
        return result
    }
}

fun IModelItem.getSbgnProperty(key: SbgnPropertyKey):Any? {
    if(tag !is SbgnData) return null
    else if((tag as SbgnData).property == null) (tag as SbgnData).property = HashMap()
    return (tag as SbgnData).property!![key]
}

fun IModelItem.setSbgnProperty(key: SbgnPropertyKey, value:Any?) {
    if(tag !is SbgnData) tag = SbgnData()
    val info = tag as SbgnData
    if(info.property == null) info.property = HashMap()
    info.property!![key] = value
}

var IModelItem.graphStyle:GraphStyle<SbgnType>?
    get() = (tag as? SbgnData)?.style
    set(value) { (tag as? SbgnData)?.let { it.style = value } }

var INode.isLocked:Boolean
    get() = getSbgnProperty(SbgnPropertyKey.IS_LOCKED) == true
    set(value) =  setSbgnProperty(SbgnPropertyKey.IS_LOCKED, value)

var INode.isClone:Boolean
    get() = getSbgnProperty(SbgnPropertyKey.IS_CLONE) == true
    set(value) =  setSbgnProperty(SbgnPropertyKey.IS_CLONE, value)

var INode.orientation:String?
    get() = getSbgnProperty(SbgnPropertyKey.ORIENTATION) as? String
    set(value) =  setSbgnProperty(SbgnPropertyKey.ORIENTATION, value)

fun INode.getNameLabel():ILabel? = labels.firstOrNull { it.type == SbgnType.NAME_LABEL }

var IModelItem.type
    get() = _getSbgnType()
    set(value) = _setSbgnType(value)

@Suppress("FunctionName")
private fun IModelItem._getSbgnType(): SbgnType {
    return if(tag is SbgnData) (tag as SbgnData).type
    else SbgnType.NO_TYPE
}

@Suppress("FunctionName")
private fun IModelItem._setSbgnType(type: SbgnType) {
    when {
        type == SbgnType.NO_TYPE -> tag = null
        tag !is SbgnData -> tag = SbgnData(type)
        else -> (tag as SbgnData).type = type
    }
}

class SbgnInfoMementoSupport : IMementoSupport {
    override fun applyState(subject: Any?, state: Any?) {
        if(subject is SbgnData && state is SbgnData) {
            subject.type = state.type
            subject.property = state.property
            subject.style = state.style
        }
    }

    override fun getState(subject: Any): Any? {
        return (subject as? SbgnData)?.clone()
    }

    override fun stateEquals(state1: Any?, state2: Any?): Boolean {
        return if(state1 is SbgnData && state2 is SbgnData) state1 == state2 else false
    }

}

class SbgnItemType : IItemType<SbgnType> {
    override fun setType(item: IModelItem, type: SbgnType) {
        item.type = type
    }
    override fun getType(item: IModelItem): SbgnType {
        return item.type
    }
}

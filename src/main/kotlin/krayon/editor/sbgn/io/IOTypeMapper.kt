/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.io

import com.yworks.yfiles.graph.IModelItem
import org.sbgn.ArcClazz
import org.sbgn.GlyphClazz
import org.sbgn.bindings.Arc
import org.sbgn.bindings.Glyph
import krayon.editor.sbgn.model.SbgnType
import krayon.editor.sbgn.model.type

object IOTypeMapper {

    private val clazzToSbgnType = mapOf(
        GlyphClazz.UNSPECIFIED_ENTITY to SbgnType.UNSPECIFIED_ENTITY,
        GlyphClazz.SIMPLE_CHEMICAL to SbgnType.SIMPLE_CHEMICAL,
        GlyphClazz.SIMPLE_CHEMICAL_MULTIMER to SbgnType.SIMPLE_CHEMICAL_MULTIMER,
        GlyphClazz.MACROMOLECULE to SbgnType.MACROMOLECULE,
        GlyphClazz.MACROMOLECULE_MULTIMER to SbgnType.MACROMOLECULE_MULTIMER,
        GlyphClazz.NUCLEIC_ACID_FEATURE to SbgnType.NUCLEIC_ACID_FEATURE,
        GlyphClazz.NUCLEIC_ACID_FEATURE_MULTIMER to SbgnType.NUCLEIC_ACID_FEATURE_MULTIMER,
        GlyphClazz.PERTURBING_AGENT to SbgnType.PERTURBING_AGENT,
        GlyphClazz.SOURCE_AND_SINK to SbgnType.SOURCE_AND_SINK,
        GlyphClazz.COMPLEX to SbgnType.COMPLEX,
        GlyphClazz.COMPLEX_MULTIMER to SbgnType.COMPLEX_MULTIMER,
        GlyphClazz.UNIT_OF_INFORMATION to SbgnType.UNIT_OF_INFORMATION,
        GlyphClazz.STATE_VARIABLE to SbgnType.STATE_VARIABLE,
        GlyphClazz.PROCESS to SbgnType.PROCESS,
        GlyphClazz.OMITTED_PROCESS to SbgnType.OMITTED_PROCESS,
        GlyphClazz.UNCERTAIN_PROCESS to SbgnType.UNCERTAIN_PROCESS,
        GlyphClazz.ASSOCIATION to SbgnType.ASSOCIATION,
        GlyphClazz.DISSOCIATION to SbgnType.DISSOCIATION,
        GlyphClazz.PHENOTYPE to SbgnType.PHENOTYPE,
        GlyphClazz.COMPARTMENT to SbgnType.COMPARTMENT,
        GlyphClazz.SUBMAP to SbgnType.SUBMAP,
        GlyphClazz.TAG to SbgnType.TAG,
        GlyphClazz.AND to SbgnType.AND,
        GlyphClazz.OR to SbgnType.OR,
        GlyphClazz.NOT to SbgnType.NOT,
        GlyphClazz.ANNOTATION to SbgnType.ANNOTATION,
        GlyphClazz.CARDINALITY to SbgnType.CARDINALITY,
        GlyphClazz.TERMINAL to SbgnType.TERMINAL,

        ArcClazz.CONSUMPTION to SbgnType.CONSUMPTION,
        ArcClazz.PRODUCTION to SbgnType.PRODUCTION,
        ArcClazz.MODULATION to SbgnType.MODULATION,
        ArcClazz.STIMULATION to SbgnType.STIMULATION,
        ArcClazz.CATALYSIS to SbgnType.CATALYSIS,
        ArcClazz.INHIBITION to SbgnType.INHIBITION,
        ArcClazz.NECESSARY_STIMULATION to SbgnType.NECESSARY_STIMULATION,
        ArcClazz.LOGIC_ARC to SbgnType.LOGIC_ARC,
        ArcClazz.EQUIVALENCE_ARC to SbgnType.EQUIVALENCE_ARC
    ).mapKeys {
        it.key.toString()
    }

    private val sbgnTypeToClazz = clazzToSbgnType.entries.associate { (k,v) -> v to k }

    fun getSbgnType(glyph:Glyph): SbgnType {
        return clazzToSbgnType[glyph.clazz] ?: SbgnType.NO_TYPE
    }

    fun getSbgnType(arc: Arc): SbgnType {
        return clazzToSbgnType[arc.clazz] ?: SbgnType.NO_TYPE
    }

    fun getGlyphClazz(item:IModelItem):String? {
        return sbgnTypeToClazz[item.type]
    }

}
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.model

enum class SbgnType {

    NO_TYPE,
    MAP,

    UNSPECIFIED_ENTITY,
    SIMPLE_CHEMICAL,
    MACROMOLECULE,
    NUCLEIC_ACID_FEATURE,
    PERTURBING_AGENT,
    SOURCE_AND_SINK,
    COMPLEX,

    SIMPLE_CHEMICAL_MULTIMER,
    MACROMOLECULE_MULTIMER,
    NUCLEIC_ACID_FEATURE_MULTIMER,
    COMPLEX_MULTIMER,

    COMPARTMENT,

    PROCESS,
    OMITTED_PROCESS,
    UNCERTAIN_PROCESS,
    ASSOCIATION,
    DISSOCIATION,
    PHENOTYPE,

    AND,
    OR,
    NOT,

    SUBMAP,
    TAG,

    CONSUMPTION,
    PRODUCTION,
    MODULATION,
    STIMULATION,
    CATALYSIS,
    INHIBITION,
    NECESSARY_STIMULATION,
    LOGIC_ARC,
    EQUIVALENCE_ARC,

    //label types
    CALLOUT_LABEL,
    STATE_VARIABLE,
    UNIT_OF_INFORMATION,
    NAME_LABEL,
    CLONE_LABEL,
    CARDINALITY,

    //port types
    TERMINAL,
    INPUT_AND_OUTPUT,

    //feature types
    CLONE_MARKER,
    MULTIMER,

    ANNOTATION;



    fun isSimpleChemical() = this == SIMPLE_CHEMICAL || this == SIMPLE_CHEMICAL_MULTIMER
    fun isMacromolecule() = this == MACROMOLECULE || this == MACROMOLECULE_MULTIMER
    fun isNucleicAcidFeature() = this == NUCLEIC_ACID_FEATURE || this == NUCLEIC_ACID_FEATURE_MULTIMER

    fun isEPN():Boolean {
        return when (this) {
            UNSPECIFIED_ENTITY, SIMPLE_CHEMICAL, MACROMOLECULE, NUCLEIC_ACID_FEATURE, PERTURBING_AGENT, SOURCE_AND_SINK, COMPLEX,
            SIMPLE_CHEMICAL_MULTIMER, MACROMOLECULE_MULTIMER, NUCLEIC_ACID_FEATURE_MULTIMER, COMPLEX_MULTIMER -> true
            else -> false
        }
    }

    fun isPN():Boolean {
        return when (this) {
            PROCESS, UNCERTAIN_PROCESS, ASSOCIATION, DISSOCIATION, OMITTED_PROCESS -> true
            else -> false
        }
    }

    fun isLogic(): Boolean {
        return when (this) {
            AND, OR, NOT -> true
            else -> false
        }
    }

    fun isReference(): Boolean {
        return when (this) {
            SUBMAP, TAG -> true
            else -> false
        }
    }

    fun canCarryCloneMarker(): Boolean {
        return when(this) {
            UNSPECIFIED_ENTITY, SIMPLE_CHEMICAL, MACROMOLECULE, NUCLEIC_ACID_FEATURE, COMPLEX, PERTURBING_AGENT, PHENOTYPE,
            SIMPLE_CHEMICAL_MULTIMER, MACROMOLECULE_MULTIMER, NUCLEIC_ACID_FEATURE_MULTIMER, COMPLEX_MULTIMER -> true
            else -> false
        }
    }

    fun canBeContainedInComplex(): Boolean {
        return this.isComplex() || this.isNucleicAcidFeature() || this.isSimpleChemical() || this.isMacromolecule() || this == UNSPECIFIED_ENTITY
    }

    fun isAuxUnit(): Boolean {
        return this == UNIT_OF_INFORMATION || this == STATE_VARIABLE
    }

    fun isComplex(): Boolean {
        return when (this) {
            COMPLEX, COMPLEX_MULTIMER -> true
            else -> false
        }
    }

    fun isMultimer(): Boolean {
        return when(this) {
            SIMPLE_CHEMICAL_MULTIMER, MACROMOLECULE_MULTIMER, NUCLEIC_ACID_FEATURE_MULTIMER, COMPLEX_MULTIMER -> true
            else -> false
        }
    }

    fun canBeMultimer(): Boolean {
        return when(this) {
            SIMPLE_CHEMICAL, MACROMOLECULE, NUCLEIC_ACID_FEATURE, COMPLEX -> true
            else -> false
        }
    }

    fun isArc(): Boolean {
        return when(this) {
            CONSUMPTION, PRODUCTION, MODULATION, STIMULATION, CATALYSIS, INHIBITION, NECESSARY_STIMULATION, LOGIC_ARC, EQUIVALENCE_ARC -> true
            else -> false
        }
    }

    fun isRegulation(): Boolean {
        return when(this) {
            CATALYSIS, INHIBITION, STIMULATION, NECESSARY_STIMULATION, MODULATION -> true
            else -> false
        }
    }

    fun isNode():Boolean {
        return isEPN() || isPN() || isLogic() || this == COMPARTMENT || this == SUBMAP
    }

    fun hasFixedPorts(): Boolean {
        return isPN() || isLogic() || this == SUBMAP
    }

    fun isRegulableProcess(): Boolean {
        return when(this) {
            PROCESS, OMITTED_PROCESS, PHENOTYPE, UNCERTAIN_PROCESS -> true
            else -> false
        }
    }
}
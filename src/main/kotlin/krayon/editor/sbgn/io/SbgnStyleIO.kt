/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.io

import krayon.editor.sbgn.model.SbgnType
import krayon.editor.base.io.StyleIO

class SbgnStyleIO: StyleIO<SbgnType>() {
    init {
        classNameEncoder = { when(it) {
            SbgnType.MAP -> null
            else -> it.name.toLowerCase().replace('_','-')
        }}
        entityNameEncoder = { when {
            it == SbgnType.MAP -> "map"
            else -> null
        }}

        classNameDecoder = {
            SbgnType.values().find { type -> classNameEncoder(type) == it }
        }

        entityNameDecoder = {
            if(it.toLowerCase() == "map") listOf(SbgnType.MAP) else null
        }
    }
}
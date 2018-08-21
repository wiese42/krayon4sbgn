/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.util

import java.util.*

interface Cache<K,V> {

    operator fun get(key:K):V?
    operator fun set(key:K, value:V)

    private class WeakCache<K,V> : Cache<K,V> {
        val map = WeakHashMap<K,V>()
        override fun get(key: K): V? {
            return map[key]
        }

        override fun set(key: K, value: V) {
            map[key] = value
            //println("size=" + map.size)
        }
    }

    private class NoCache<K,V> : Cache<K,V> {
        override fun get(key: K): V? {
            return null
        }

        override fun set(key: K, value: V) {

        }
    }

    companion object {
        fun <K,V> createWeakCache():Cache<K,V> = WeakCache()
        @Suppress("unused")
        fun <K,V> createNoCache():Cache<K,V> = NoCache()
    }
}
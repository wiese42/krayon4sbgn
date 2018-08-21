/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.util

import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun NodeList.asList():List<Node> {
    return NodeListWrapper(this)
}

private class NodeListWrapper(val list:NodeList) : AbstractList<Node>() {
    override val size: Int = list.length
    override fun get(index: Int): Node = list.item(index)
}

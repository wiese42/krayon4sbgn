/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.io

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.XMLFilterImpl

class NamespaceFilter(namespaceUri: String,
                      private val addNamespace: Boolean = true) : XMLFilterImpl() {

    private var usedNamespaceUri: String? = null
    private var addedNamespace = false

    init {
        if (addNamespace)
            this.usedNamespaceUri = namespaceUri
        else
            this.usedNamespaceUri = ""
    }


    @Throws(SAXException::class)
    override fun startDocument() {
        super.startDocument()
        if (addNamespace) {
            startControlledPrefixMapping()
        }
    }


    @Throws(SAXException::class)
    override fun startElement(arg0: String, arg1: String, arg2: String,
                              arg3: Attributes) {
        if(arg0.startsWith("http://sbgn.org/libsbgn/"))
            super.startElement(this.usedNamespaceUri, arg1, arg2, arg3)
        else
            super.startElement(arg0, arg1, arg2, arg3)
    }

    @Throws(SAXException::class)
    override fun endElement(arg0: String, arg1: String, arg2: String) {

        super.endElement(this.usedNamespaceUri, arg1, arg2)
    }

    @Throws(SAXException::class)
    override fun startPrefixMapping(prefix: String, url: String) {
        if (addNamespace) {
            this.startControlledPrefixMapping()
        } else {
            //Remove the namespace, i.e. don´t call startPrefixMapping for parent!
        }

    }

    @Throws(SAXException::class)
    private fun startControlledPrefixMapping() {

        if (this.addNamespace && !this.addedNamespace) {
            //We should add namespace since it is set and has not yet been done.
            super.startPrefixMapping("", this.usedNamespaceUri)

            //Make sure we dont do it twice
            this.addedNamespace = true
        }
    }

}
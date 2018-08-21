/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.command

import com.yworks.yfiles.view.GraphComponent
import krayon.editor.base.ui.IGraphComponentFactory
import krayon.editor.base.ui.PrintPreview

object PrintPreview : ApplicationCommand("PRINT_PREVIEW") {

    private val printPreview by lazy {
        PrintPreview(graphComponent).apply {
            graphComponentFactory = { graphComponent.lookup(IGraphComponentFactory::class.java)?.createGraphComponent() ?: GraphComponent() }
        }
    }
    private val dialog by lazy {
        printPreview.createDialog("Print Preview")
    }

    override fun canExecute(param: Any?) = graph.nodes.any()

    override fun execute(param:Any?) {
        printPreview.updatePrintPreview()
        dialog.isVisible = true
    }
}
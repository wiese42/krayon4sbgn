/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.config

import com.yworks.yfiles.geometry.InsetsD
import krayon.editor.base.ui.PrintPreview
import yfiles.demo.toolkit.optionhandler.EnumValueAnnotation
import yfiles.demo.toolkit.optionhandler.EnumValueAnnotations
import yfiles.demo.toolkit.optionhandler.Label
import yfiles.demo.toolkit.optionhandler.OptionGroupAnnotation


@Label("Print Settings")
open class PrintConfiguration(private val printPreview: PrintPreview) {

    enum class ExportArea {
        COMPLETE_DIAGRAM,
        VISIBLE_IN_VIEW
    }
    
    @get:Label("Export Area")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 5)
    @get:EnumValueAnnotations(
            EnumValueAnnotation(label = "Complete Diagram", value = "COMPLETE_DIAGRAM"),
            EnumValueAnnotation(label = "Visible in View", value = "VISIBLE_IN_VIEW")
    )
    var exportArea: ExportArea = ExportArea.COMPLETE_DIAGRAM

    @get:Label("Print Decorations")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 10)
    var printDecorations = false

    @get:Label("Center Content")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 20)
    var centerContent
        get() = printPreview.canvasPrintable.isCenteringContentEnabled
        set(value) { printPreview.canvasPrintable.isCenteringContentEnabled = value }

    @get:Label("Scale Down to Fit Page")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 30)
    var scaleDown
        get() = printPreview.canvasPrintable.isScalingDownToFitPageEnabled
        set(value) { printPreview.canvasPrintable.isScalingDownToFitPageEnabled = value }

    @get:Label("Scale Up to Fit Page")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 40)
    var scaleUp
        get() = printPreview.canvasPrintable.isScalingUpToFitPageEnabled
        set(value) { printPreview.canvasPrintable.isScalingUpToFitPageEnabled = value }

    @get:Label("Print Page Marks")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 50)
    var isPageMarkPrintingEnabled: Boolean
        get() = printPreview.canvasPrintable.isPageMarkPrintingEnabled
        set(enabled) = if (enabled) {
            printPreview.canvasPrintable.isPageMarkPrintingEnabled = true
            printPreview.canvasPrintable.contentMargins = printPreview.newPrintMarksMargins(printPreview.pageFormat)
        } else {
            printPreview.canvasPrintable.isPageMarkPrintingEnabled = false
            printPreview.canvasPrintable.contentMargins = InsetsD(0.0)
        }


}

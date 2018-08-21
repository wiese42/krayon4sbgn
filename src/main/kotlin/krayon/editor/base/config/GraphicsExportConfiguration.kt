/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.config

import krayon.editor.base.ui.ExtensionFileFilter
import yfiles.demo.toolkit.optionhandler.*
import javax.swing.filechooser.FileFilter


@Label("Export to Graphics Format")
open class GraphicsExportConfiguration {

    enum class ExportFormat(private val description: String, val supportsTransparency: Boolean, val isQualityAdjustable: Boolean, private vararg val extension: String) {

        PNG("PNG Files", true, false, "png"),
        JPG("JPEG Files", false, true, "jpg", "jpeg", "jpe"),
        GIF("GIF Files", false, false, "gif"),
        SVG("SVG Files", true, false, "svg", "svgz"),
        EPS("EPS Files", true, false, "eps"),
        PDF("PDF Files", true, false, "pdf");

        val fileFilter: FileFilter get() = ExtensionFileFilter(description, *extension)

        internal fun canonicalExtension() = extension[0]

        override fun toString(): String {
            return canonicalExtension().toUpperCase()
        }
    }

    enum class ExportArea {
        COMPLETE_DIAGRAM,
        VISIBLE_IN_VIEW
    }

    enum class SizeMode {
        USE_ORIGINAL_SIZE,
        SPECIFY_WIDTH,
        SPECIFY_HEIGHT
    }

    @get:Label("Export Format")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 0)
    @get:EnumValueAnnotations(
            EnumValueAnnotation(label = "PNG Image", value = "PNG"),
            EnumValueAnnotation(label = "JPG Image", value = "JPG"),
            EnumValueAnnotation(label = "GIF Image", value = "GIF"),
            EnumValueAnnotation(label = "SVG Vector Graphics", value = "SVG"),
            EnumValueAnnotation(label = "PDF Portable Document Format", value = "PDF"),
            EnumValueAnnotation(label = "EPS Encapsulated Postscript", value = "EPS")
    )
    var format: ExportFormat = ExportFormat.PNG

    @get:Label("Export Area")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 5)
    @get:EnumValueAnnotations(
            EnumValueAnnotation(label = "Complete Diagram", value = "COMPLETE_DIAGRAM"),
            EnumValueAnnotation(label = "Visible in View", value = "VISIBLE_IN_VIEW")
    )
    var exportArea: ExportArea = ExportArea.COMPLETE_DIAGRAM

    @get:Label("Size")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 10)
    @get:EnumValueAnnotations(
            EnumValueAnnotation(label = "Use Original Size", value = "USE_ORIGINAL_SIZE"),
            EnumValueAnnotation(label = "Specify Height", value = "SPECIFY_HEIGHT"),
            EnumValueAnnotation(label = "Specify Width", value = "SPECIFY_WIDTH")
    )
    var sizeMode: SizeMode = SizeMode.USE_ORIGINAL_SIZE


    @get:Label("Custom Width")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 20)
    var customWidth = 500
    val isShouldDisableCustomWidth get() = sizeMode != SizeMode.SPECIFY_WIDTH

    @get:Label("Custom Height")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 30)
    var customHeight = 500
    val isShouldDisableCustomHeight get() = sizeMode != SizeMode.SPECIFY_HEIGHT

    //@get:Label("Show Decorations")
    //@get:OptionGroupAnnotation(name = "RootGroup", position = 40)
    private var showDecorations = false //use new view

    @get:Label("Transparent Background")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 50)
    var transparent = false

    @get:Label("Margin")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 60)
    var margin = 20
    val isShouldDisableMargin get() = exportArea == ExportArea.VISIBLE_IN_VIEW

    @get:Label("Scale Image Size (%)")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 70)
    @get:MinMax(min = 1.0, max = 1000.0)
    @get:ComponentType(ComponentTypes.SLIDER)
    var scale:Int = 100
    val isShouldDisableScale get() = sizeMode != SizeMode.USE_ORIGINAL_SIZE

    @get:Label("Image Quality (%)")
    @get:OptionGroupAnnotation(name = "RootGroup", position = 80)
    @get:ComponentType(ComponentTypes.SLIDER)
    @get:MinMax(min = 10.0, max = 100.0)
    var imageQuality = 100
    val isShouldDisableImageQuality get() = !format.isQualityAdjustable


}

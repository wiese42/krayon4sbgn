
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.InsetsD
import com.yworks.yfiles.view.*
import org.apache.batik.dom.svg.SVGDOMImplementation
import org.apache.batik.svggen.SVGGraphics2D
import org.apache.batik.util.XMLConstants
import org.freehep.graphics2d.VectorGraphics
import org.freehep.graphicsbase.util.UserProperties
import org.freehep.graphicsio.pdf.PDFGraphics2D
import org.freehep.graphicsio.ps.EPSGraphics2D
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Element
import org.w3c.dom.svg.SVGDocument
import krayon.editor.base.config.GraphicsExportConfiguration
import yfiles.demo.toolkit.optionhandler.OptionEditor
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.*
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.swing.*
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class GraphicsExport(val graphComponent: GraphComponent)  {

    private val config: GraphicsExportConfiguration = GraphicsExportConfiguration()

    var graphComponentFactory:() -> GraphComponent = { GraphComponent() }

    private var previewPanel: JLabel? = null

    private var closeListeners = mutableListOf<() -> Unit>()

    private fun updatePreview() {
        previewPanel?.let {
            if(it.size.height <= 0) return
            if(it.size.height <= 0) return
            val image = createPreviewImage(it.size)
            it.icon = ImageIcon(image)
        }
    }

    private fun createOptionPane():JPanel {
        val previewPanel = JLabel().apply {
            verticalAlignment = SwingConstants.CENTER
            horizontalAlignment = SwingConstants.CENTER
            isOpaque = true
            background = Color.LIGHT_GRAY
            preferredSize = Dimension(600,400)
        }


        val builder = OptionEditor()
        builder.configuration = config
        val editorPane = builder.buildEditor(::updatePreview)
        editorPane.border = emptyBorder(10)

        val optionPane = JPanel(BorderLayout()).apply {
            border = emptyBorder(6)
            add(previewPanel, BorderLayout.CENTER)
            add(editorPane,BorderLayout.WEST)
        }

        val buttonBar = JPanel(FlowLayout()).apply { border = emptyBorder(10, 0, 10, 0) }
        buttonBar.add(createButton("Export...") {
            if(saveToFile()) onClose()
        })
        buttonBar.add(createButton("Close") {
            onClose() }
        )

        optionPane.add(buttonBar,BorderLayout.SOUTH)

        this.previewPanel = previewPanel
        return optionPane
    }

    private fun onClose() {
        closeListeners.forEach { it.invoke() }
    }

    private fun createButton(name:String, action:(ActionEvent) -> Unit):JButton {
        return JButton(object:AbstractAction(name) {
            override fun actionPerformed(e: ActionEvent) { action(e) }
        })
    }

    private fun createPreviewImage(imageSize:Dimension): BufferedImage {
        val component = getExportingGraphComponent()
        val exporter = getPixelImageExporter()
        val width = exporter.configuration.viewWidth
        val height = exporter.configuration.viewHeight
        val wScale = imageSize.width.toDouble() / width
        val hScale = imageSize.height.toDouble() / height
        val scale = Math.min(if(wScale < hScale) wScale else hScale, 1.0)
        val configScale = exporter.configuration.scale
        exporter.configuration.scale *= scale
        val image = exporter.exportToBitmap(component)
        exporter.configuration.scale = configScale
        return image
    }

    private fun exportComponentToStream(component: GraphComponent, exporter: PixelImageExporter, stream: OutputStream) {
        //println("exportComponentToStream size= " + component.viewport.size)
        //println("exportComponentToStream size= " + component.viewport.size)
        with(config) {
            if (format.isQualityAdjustable) {
                // adjust the compression imageQuality for jpg format
                val writer = ImageIO.getImageWritersByFormatName(format.canonicalExtension()).next()
                val param = writer.defaultWriteParam
                param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                param.compressionQuality = config.imageQuality.toFloat()
                try {
                    exporter.export(component, stream, writer, param)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    writer.dispose()
                }
            } else {
                try {
                    exporter.export(component, stream, format.canonicalExtension())
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun createContextConfigurator(): ContextConfigurator {
        // check if the rectangular region or the whole view port should be printed
        //val regionToExport = if (useRectangle) exportRect.toRectD() else getExportingGraphComponent().getViewport()
        with(config) {
            val regionToExport = when (exportArea) {
                GraphicsExportConfiguration.ExportArea.VISIBLE_IN_VIEW -> getExportingGraphComponent().viewport
                GraphicsExportConfiguration.ExportArea.COMPLETE_DIAGRAM -> {
                    getExportingGraphComponent().updateContentRect()
                    getExportingGraphComponent().contentRect.getEnlarged(2.0)
                }
            }

            // create a configurator with the settings of the option panel
            val configurator = ContextConfigurator(regionToExport)
            if (exportArea != GraphicsExportConfiguration.ExportArea.VISIBLE_IN_VIEW) configurator.margins = InsetsD(margin.toDouble())
            setScale(configurator)
            return configurator
        }
    }

    private fun setScale(configurator: ContextConfigurator) {
        with(config) {
            var zoomedScale = (scale.toDouble()) * 0.01

            if(exportArea == GraphicsExportConfiguration.ExportArea.VISIBLE_IN_VIEW) zoomedScale *= graphComponent.zoom

            when (sizeMode) {
                GraphicsExportConfiguration.SizeMode.SPECIFY_WIDTH -> zoomedScale = configurator.calculateScaleForWidth(customWidth.toDouble() - configurator.margins.horizontalInsets)
                GraphicsExportConfiguration.SizeMode.SPECIFY_HEIGHT -> zoomedScale = configurator.calculateScaleForHeight(customHeight.toDouble() - configurator.margins.verticalInsets)
                else -> {}
            }
            configurator.scale = if(zoomedScale <= 0.0) 1.0 else zoomedScale
        }
    }


    /**
     * Returns the component to export from. For exporting an 'undecorated' image, we use a new one.
     */
    private fun getExportingGraphComponent(): GraphComponent {
        //return if (config.showDecorations) graphComponent else GraphComponent().apply {
        return graphComponentFactory().apply {
            size = graphComponent.size
            graph = graphComponent.graph
            background = graphComponent.background
            zoom = graphComponent.zoom
            viewPoint = graphComponent.viewPoint
            updateContentRect()
            repaint()
        }
    }

    private fun getPixelImageExporter(): PixelImageExporter {
        with(config) {
            // create an exporter with the settings of the option panel
            val configurator = createContextConfigurator()

            val exporter = PixelImageExporter(configurator)
            // check if the format is transparent PNG
            if (format.supportsTransparency && transparent) {
                exporter.backgroundFill = Colors.TRANSPARENT
                exporter.isUsingCanvasComponentBackgroundColorEnabled = false
                exporter.isTransparencyEnabled = transparent
            } else {
                exporter.isUsingCanvasComponentBackgroundColorEnabled = true
                exporter.isTransparencyEnabled = false
            }
            return exporter
        }
    }

    private val fileChooser:JFileChooser by lazy {
        JFileChooser().apply {
            isAcceptAllFileFilterUsed = false
        }
    }

    private fun saveToFile(_fileName:String? = null):Boolean {

        val format = config.format

        fileChooser.apply {
            dialogTitle = "Export Diagram"
            dialogType = JFileChooser.SAVE_DIALOG
            resetChoosableFileFilters()
            addChoosableFileFilter(format.fileFilter)
        }

        var fileName = _fileName ?:
        if(fileChooser.showSaveDialogFX(graphComponent) == JFileChooser.APPROVE_OPTION) {
            fileChooser.selectedFile.absolutePath
        }
        else return false

        if(fileChooser.choosableFileFilters.none { it.accept(File(fileName)) } ) fileName += format.canonicalExtension()

        return when(format) {
            GraphicsExportConfiguration.ExportFormat.SVG -> {
                // export to an SVG element
                val svgRoot = exportToSVGElement()
                val svgDocumentFragment = svgRoot.ownerDocument.createDocumentFragment()
                svgDocumentFragment.appendChild(svgRoot)

                val fout = FileOutputStream(fileName)
                val stream = if(fileName.endsWith(".svgz")) GZIPOutputStream(fout) else fout

                try {
                    stream.use { writeSvgDocument(svgDocumentFragment, OutputStreamWriter(it, "UTF-8")) }
                    true
                } catch (e: IOException) {
                    e.printStackTrace()
                    false
                }
            }
            GraphicsExportConfiguration.ExportFormat.PDF -> {
                try {
                    FileOutputStream(fileName).use { stream -> exportPdf(stream) }
                    true
                } catch (e: IOException) {
                    e.printStackTrace()
                    false
                }
            }
            else -> {
                val component = getExportingGraphComponent()
                val exporter = getPixelImageExporter()
                try {
                    val stream = FileOutputStream(fileName)
                    exportComponentToStream(component, exporter, stream)
                    stream.close()
                    true
                } catch (e: IOException) {
                    e.printStackTrace()
                    false
                }
            }
        }
    }

    // SVG stuff
    private fun exportToSVGElement(): Element {
        // Create a SVG document.
        val impl = SVGDOMImplementation.getDOMImplementation()
        val svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI
        val doc = impl.createDocument(svgNS, "svg", null) as SVGDocument

        // Create a converter for this document.
        val svgGraphics2D = SVGGraphics2D(doc)

        // paintSvg the content of the exporting graph component to the Graphics object
        paintSvg(getExportingGraphComponent(), svgGraphics2D)

        svgGraphics2D.dispose()
        val svgRoot = svgGraphics2D.getRoot(doc.documentElement)
        svgRoot.setAttributeNS(XMLConstants.XMLNS_NAMESPACE_URI, XMLConstants.XMLNS_PREFIX + ":"
                + XMLConstants.XLINK_PREFIX, XMLConstants.XLINK_NAMESPACE_URI)
        return svgRoot
    }

    @Throws(IOException::class)
    private fun writeSvgDocument(svgDocument: DocumentFragment, writer: Writer) {
        try {
            // Prepare the DOM document for writing
            val source = DOMSource(svgDocument)
            val result = StreamResult(writer)

            // Write the DOM document to the file
            val tf = TransformerFactory.newInstance()
            try {
                tf.setAttribute("indent-number", 2)
            } catch (iaex: IllegalArgumentException) {
                iaex.printStackTrace()
            }

            val xformer = tf.newTransformer()
            xformer.setOutputProperty(OutputKeys.INDENT, "yes")
            xformer.transform(source, result)
        } catch (e: TransformerException) {
            throw IOException(e.message)
        }
    }

    /**
     * Paints the canvas on the provided graphics context.
     */
    private fun paintSvg(canvas: CanvasComponent, gfx: Graphics2D) {
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val cnfg = createContextConfigurator()
        val graphics = gfx.create() as Graphics2D
        try {
            // fill background
            val fill = if (config.transparent) Colors.TRANSPARENT else canvas.background
            if (fill != null) {
                val oldPaint = graphics.paint
                graphics.paint = fill
                graphics.fill(Rectangle2D.Double(0.0, 0.0, cnfg.viewWidth.toDouble(), cnfg.viewHeight.toDouble()))
                graphics.paint = oldPaint
            }

            // configure the Graphics transform
            val margins = cnfg.margins
            graphics.translate(margins.getLeft(), margins.getTop())
            val paintContext = cnfg.createRenderContext(canvas)
            graphics.transform(paintContext.toWorldTransform)

            // set the graphics clip
            val clip = paintContext.clip
            if (clip != null) {
                graphics.clip(Rectangle2D.Double(clip.getX(), clip.getY(), clip.getWidth(), clip.getHeight()))
            }

            // export the canvas content
            canvas.exportContent(paintContext).paint(paintContext, graphics)
        } finally {
            graphics.dispose()
        }
    }

    // Pdf stuff

    private fun exportPdf(os: OutputStream) {
        val currentFormat = config.format
        val currentTransparent = currentFormat.supportsTransparency && config.transparent

        val canvas = getExportingGraphComponent()
        val cnfg = createContextConfigurator()
        val size = Dimension(cnfg.viewWidth, cnfg.viewHeight)

        // create and initialize the VectorGraphics
        val gfx = when (currentFormat) {
            GraphicsExportConfiguration.ExportFormat.EPS -> createEpsGraphics(os, size)
            else -> createPdfGraphics(os, size)
        }

        gfx.startExport()

        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        gfx.create { graphics ->
            // fill background
            val fill = if (currentTransparent) Colors.TRANSPARENT else canvas.background
            if (fill != null) {
                val oldPaint = graphics.paint
                graphics.paint = fill
                graphics.fill(Rectangle2D.Double(0.0, 0.0, cnfg.viewWidth.toDouble(), cnfg.viewHeight.toDouble()))
                graphics.paint = oldPaint
            }

            // configure the Graphics transform
            val margins = cnfg.margins
            graphics.translate(margins.getLeft(), margins.getTop())
            val paintContext = cnfg.createRenderContext(canvas)
            graphics.transform(paintContext.toWorldTransform)

            // set the graphics clip
            val clip = paintContext.clip
            if (clip != null) {
                graphics.clip(Rectangle2D.Double(clip.getX(), clip.getY(), clip.getWidth(), clip.getHeight()))
            }

            // export the canvas content
            canvas.exportContent(paintContext).paint(paintContext, graphics)
        }

        gfx.endExport()
    }

    private fun createPdfGraphics(os: OutputStream, size: Dimension): VectorGraphics{
        // create export properties
        val properties = Properties().apply {
            putAll(PDFGraphics2D.getDefaultProperties())
            setProperty(PDFGraphics2D.PAGE_SIZE, PDFGraphics2D.CUSTOM_PAGE_SIZE)
            setProperty(PDFGraphics2D.CUSTOM_PAGE_SIZE, size.width.toString() + ", " + size.height)
        }

        UserProperties.setProperty(properties, PDFGraphics2D.PAGE_MARGINS, Insets(0, 0, 0, 0))
        UserProperties.setProperty(properties, PDFGraphics2D.FIT_TO_PAGE, false)

        return PDFGraphics2D(os, size).apply {
            setProperties(properties)
        }
    }

    private fun createEpsGraphics(os: OutputStream, size: Dimension): EPSGraphics2D {
        // create export properties
        val properties = Properties().apply {
            putAll(EPSGraphics2D.getDefaultProperties())
            setProperty(EPSGraphics2D.PAGE_SIZE, EPSGraphics2D.CUSTOM_PAGE_SIZE)
            setProperty(EPSGraphics2D.CUSTOM_PAGE_SIZE, size.width.toString() + ", " + size.height)
        }
        UserProperties.setProperty(properties, EPSGraphics2D.PAGE_MARGINS, Insets(0, 0, 0, 0))
        UserProperties.setProperty(properties, EPSGraphics2D.FIT_TO_PAGE, false)

        return EPSGraphics2D(os, size).apply {
            setProperties(properties)
        }
    }

    fun createDialog(title:String):JDialog {
        return JDialog(SwingUtilities.getWindowAncestor(graphComponent),title,Dialog.ModalityType.APPLICATION_MODAL).apply {
            contentPane = createOptionPane()
            defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
            closeListeners.add { isVisible = false }
            pack()
            updatePreview()
        }
    }
}

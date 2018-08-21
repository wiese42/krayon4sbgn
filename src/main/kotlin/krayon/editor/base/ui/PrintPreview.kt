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
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.view.CanvasComponent
import com.yworks.yfiles.view.CanvasPrintable
import com.yworks.yfiles.view.GraphComponent
import krayon.editor.base.config.PrintConfiguration
import krayon.editor.base.util.IconManager
import yfiles.demo.toolkit.optionhandler.OptionEditor
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dialog
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.print.*
import javax.swing.*

class PrintPreview(val graphComponent: GraphComponent) {

    internal val canvasPrintable = CanvasPrintable(graphComponent)
    private val contentPane: JPanel
    private lateinit var pagePanel: PagePanel
    private lateinit var pageScrollPane: JScrollPane
    private lateinit var config: PrintConfiguration
    private val printerJob: PrinterJob = PrinterJob.getPrinterJob()

    private enum class FitPolicy {
        FIT,
        FIT_WIDTH,
        NONE
    }
    private var fitPolicy:FitPolicy = FitPolicy.FIT

    var graphComponentFactory: () -> GraphComponent = { GraphComponent() }

    var pageFormat: PageFormat = PageFormat()
        set(value) {
            if (value !== field) {
                field = value
                this.pagePanel.pageFormat = value
                if (canvasPrintable.isPageMarkPrintingEnabled) {
                    canvasPrintable.contentMargins = newPrintMarksMargins(value)
                }
                onPageFormatChanged()
            }
            updateZoom()
        }

    init {

        // setup the PrinterJob to use the correct printable, page format and to query the correct number of pages.
        configurePrinterJob()

        contentPane = createContentPane()
        contentPane.addComponentListener(object: ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                updateZoom()
            }
        })
    }

    /**
     * Configures the PrinterJob to use our printable and page format.
     */
    private fun configurePrinterJob() {
        this.printerJob.setPageable(object : Pageable {
            override fun getNumberOfPages(): Int {
                // query the number of pages from the CanvasPrintable with the current set PageFormat
                return canvasPrintable.pageCount(this@PrintPreview.pageFormat)
            }

            @Throws(IndexOutOfBoundsException::class)
            override fun getPageFormat(pageIndex: Int): PageFormat? {
                // delegate to the pageFormat property of the print preview
                return this@PrintPreview.pageFormat
            }

            @Throws(IndexOutOfBoundsException::class)
            override fun getPrintable(pageIndex: Int): Printable {
                return canvasPrintable
            }
        })
    }

    /**
     * Initializes the content pane and builds the toolbar as well as
     * the paper panel that is the actual preview of the printed papers.
     */
    private fun createContentPane():JPanel {
        // our main container is a common JPanel with a border layout.
        val contentPane = JPanel(BorderLayout())

        // creates and configures the toolbar with buttons to change settings, zoom in and out and print.
        val toolBar = createToolbar()
        contentPane.add(toolBar, if(toolBar.orientation == JToolBar.VERTICAL) BorderLayout.WEST else BorderLayout.NORTH)

        // create the preview that displays the papers that would be printed.
        addPaperPanel(contentPane)

        config = PrintConfiguration(this)
        val builder = OptionEditor().apply { configuration = config }
        val optionPane = builder.buildEditor { updatePrintPreview() }
        optionPane.border = emptyBorder(5)
        contentPane.add(optionPane, BorderLayout.WEST)

        return contentPane
    }

    /**
     * Creates the toolbar that contains buttons to control the page panel and
     * to change settings like the PageFormat. Also contains the print button.
     */
    private fun createToolbar():JToolBar {
        val toolBar = JToolBar()

        toolBar.add(UiFactory.createAction { queryPageFormat() }.apply { tooltip = "Adjust page format"; icon = IconManager.iconMap["ICON.PAGE_FORMAT"]?.icon32 })

        toolBar.add(UiFactory.createAction { pagePanel.zoom *= 1.2; }.apply { tooltip = "Zoom in"; icon = IconManager.iconMap["ICON.ZOOM_IN"]?.icon32  })
        toolBar.add(UiFactory.createAction { pagePanel.zoom /= 1.2; }.apply { tooltip = "Zoom out"; icon = IconManager.iconMap["ICON.ZOOM_OUT"]?.icon32 })

        toolBar.add(UiFactory.createAction { zoomToFit(); fitPolicy=FitPolicy.FIT }.apply {
            tooltip = "Zoom to fit page"
            icon = IconManager.iconMap["ICON.ZOOM_TO_FIT_PAGE"]?.icon32})
        toolBar.add(UiFactory.createAction { zoomToFitWidth(); fitPolicy=FitPolicy.FIT_WIDTH }.apply {
            tooltip = "Zoom to fit page width"
            icon = IconManager.iconMap["ICON.ZOOM_TO_FIT_PAGE_WIDTH"]?.icon32 }
        )

        return toolBar
    }

    /**
     * Creates and configures the preview panel that displays the papers to be printed.
     */
    private fun addPaperPanel(contentPane:JPanel) {
        val paperPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEtchedBorder()
        }
        this.pagePanel = PagePanel(this.canvasPrintable, this.pageFormat)
        // put the page panel into a scrollpane, it implements Scrollable
        this.pageScrollPane = JScrollPane(this.pagePanel)
        paperPanel.add(pageScrollPane, BorderLayout.CENTER)
        this.pageScrollPane.preferredSize = Dimension(400, 500)

        // wire up the combobox that contains the various zoom levels. when a zoom level is selected the page panel is updated.
        //this.zoomBox.addActionListener { onZoomChanged() }
        contentPane.add(paperPanel, BorderLayout.CENTER)
    }

    /**
     * Called when an item in the combobox that contains various zoom levels is selected.
     */
    fun updateZoom() {
        when(fitPolicy) {
            FitPolicy.FIT -> zoomToFit()
            FitPolicy.FIT_WIDTH -> zoomToFitWidth()
            else -> {}
//            else -> {
//                // parse the zoom of the string and update the page panel
//                var text = zoomBox.selectedItem.toString().trim { it <= ' ' }
//                text = text.substring(0, text.indexOf('%')).trim { it <= ' ' }
//                val zoomPercent = java.lang.Double.parseDouble(text)
//                pagePanel.zoom = zoomPercent / 100.0
//            }
        }
    }

    /**
     * Updates the content of the preview panel with the given CanvasComponent and area to print.
     */
    fun update(componentToPrint: CanvasComponent, printRectangle: RectD) {
        // update the printable with the new settings
        this.canvasPrintable.canvas = componentToPrint
        this.canvasPrintable.printRectangle = printRectangle
        this.canvasPrintable.scale = componentToPrint.zoom
        this.canvasPrintable.reset()

        // notify the page panel that something has has changed
        updatePagePanel()
    }

    /**
     * Updates the page panel to the current values of the PageFormat, zoom and CanvasPrintable.
     */
    private fun updatePagePanel() {
        this.pagePanel.updatePrintInfo()
        this.pagePanel.adjust()
    }

    /**
     * Called when the page format was changed to update the preview with the new values.
     * This default implementation calls [.update]
     * with the values already present in the `canvasPrintable`
     * and repaints the content pane.
     */
    private fun onPageFormatChanged() {
        // notify the page panel that something has has changed
        update(this.canvasPrintable.canvas, this.canvasPrintable.printRectangle)
        this.contentPane.repaint()
    }

    /**
     * Zooms the document to make it fit the preview panel.
     */
    private fun zoomToFit() {
        val width = this.pageScrollPane.viewport.width - 3
        val height = this.pageScrollPane.viewport.height - 3
        this.pagePanel.zoomToFit(width, height)
    }

    /**
     * Zooms the document to make its width fit the preview panel.
     */
    private fun zoomToFitWidth() {
        val width = this.pageScrollPane.viewport.width - 20
        this.pagePanel.zoomToFitWidth(width)
    }

    /**
     * Pops up the page dialog of the PrinterJob to query a new PageFormat.
     */
    private fun queryPageFormat() {
        pageFormat = printerJob.pageDialog(pageFormat)
    }

    /**
     * Starts the printing.
     */
    private fun print():Boolean {
        if (printerJob.printDialog()) {
            try {
                printerJob.print()
                return true
            } catch (e: PrinterAbortException) {
                // don't show an error because this exception is typically thrown when the user canceled printing
            } catch (pe: PrinterException) {
                JOptionPane.showMessageDialog(contentPane,
                        "Printing failed." + if (pe.message != null) " Reason: " + pe.message else "",
                        "Printing Error", JOptionPane.ERROR_MESSAGE)
            }
        }
        return false
    }

    /**
     * Creates insets that extend the margins of the given page format by half an
     * inch.
     */
    internal fun newPrintMarksMargins(page: PageFormat): InsetsD {
        val top = page.imageableY
        val left = page.imageableX
        val bottom = page.height - top - page.imageableHeight
        val right = page.width - left - page.imageableWidth

        val markSize = 36.0 // Java print API works at 72 dpi
        return InsetsD(top + markSize, left + markSize, bottom + markSize, right + markSize)
    }

    fun updatePrintPreview() {
        val bounds = getPrintableArea()
        update(getGraphComponentToPrint(), bounds.getEnlarged(-2.0))
        zoomToFit()
    }

    private fun getPrintableArea(): RectD {
        return when(config.exportArea) {
            PrintConfiguration.ExportArea.VISIBLE_IN_VIEW -> graphComponent.viewport
            else -> {
                graphComponent.updateContentRect()
                graphComponent.contentRect.getEnlarged(2.0)
            }
        }
    }

    /**
     * Returns the component to use for printing.
     * For printing an 'undecorated' graph (hideDecorations == true), we use fresh instance one.
     */
    private fun getGraphComponentToPrint(): GraphComponent {
        return if(config.printDecorations) graphComponent else {
            graphComponentFactory().apply {
                size = graphComponent.size
                graph = graphComponent.graph
                viewPoint = graphComponent.viewPoint
                zoom = graphComponent.zoom
                updateContentRect()
                repaint()
            }
        }
    }

    fun createDialog(title:String):JDialog {
        return JDialog(SwingUtilities.getWindowAncestor(graphComponent),title,Dialog.ModalityType.APPLICATION_MODAL).apply {
            defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
            contentPane.apply {
                layout = BorderLayout()
                add(this@PrintPreview.contentPane, BorderLayout.CENTER)
                val buttonPane = JPanel()
                buttonPane.add(JButton(UiFactory.createAction {
                    if(print()) {
                        SwingUtilities.getWindowAncestor(it.source as Component).isVisible = false
                    }
                }.apply { tooltip = "Print with the current settings"; name = "Print..." }))

                buttonPane.add(JButton("Cancel").apply {
                    addActionListener {
                        SwingUtilities.getWindowAncestor(it.source as Component).isVisible = false
                    }
                })
                add(buttonPane, BorderLayout.SOUTH)
            }
            pack()
        }
    }
}

/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.view.CanvasPrintable
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.print.PageFormat
import java.awt.print.Printable
import javax.swing.JPanel
import javax.swing.Scrollable

/**
 * A JPanel that previews the current output of a given CanvasPrintable. The preview consists of a grid of rectangles representing the papers that
 * would be printed with a given PageFormat.
 */
class PagePanel
/**
 * Creates a new PagePanel and initializes the PrintInfo, minimumSize of the component and the zoom using the given values.
 * @param printable the printable that will provide the contents of the printing.
 * @param pageFormat the PageFormat to use for the papers to print.
 */
(
        /**
         * The printable that will do the actual printing. Used to fill the content of the paper previews in the panel.
         */
        private val printable: CanvasPrintable,
        /**
         * The PageFormat in which the pages will be printed.
         */
        /**
         * Returns the current set PageFormat.
         */
        /**
         * Sets the PageFormat to use for the papers in the preview.
         */
        var pageFormat: PageFormat?) : JPanel(), Scrollable {

    /**
     * The PrintInfo from the CanvasPrintable which tells the PagePanel how many columns and rows there are to paint.
     */
    private var printInfo: CanvasPrintable.PrintInfo? = null

    /**
     * The zoom level of the panel.
     */
    /**
     * Gets the zoom. The default value is `0.5`.
     */
    /**
     * Sets the zoom. Triggers a call to adjust() to make the changes visible.
     * The default value is `0.5`.
     */
    var zoom: Double = 0.toDouble()
        set(value) {
            var zoom = value
            if (this.zoom != zoom) {
                if (zoom < 0.05) {
                    zoom = 0.05
                }
                field = zoom
                adjust()
            }
        }

    init {
        // determine the number of columns and rows there are to paint
        updatePrintInfo()
        // calculate and set the minimum size of the component (the size where all papers have zero width and height)
        minimumSize = Dimension(printInfo!!.columnCount * 2 * DESKTOP_INSETS + DROP_SHADOW_OFFSET,
                printInfo!!.rowCount * 2 * DESKTOP_INSETS + DROP_SHADOW_OFFSET)
        // init the zoom to some reasonable amount
        zoom = 0.5
    }

    /**
     * Calculates the zoom that would be necessary to fit the current contents of the preview into the given width
     * and sets the zoom for this panel accordingly. The minimum value set is 0.05.
     */
    fun zoomToFitWidth(width: Int) {
        val widthRatio = getWidthRatio(width)
        zoom = Math.max(0.05, widthRatio)
    }

    /**
     * Calculates the zoom that would be necessary to fit the current contents of the preview into the given width or height, whichever is smaller.
     * The minimum returned value is 0.05.
     */
    fun zoomToFit(width: Int, height: Int) {
        val widthRatio = getWidthRatio(width)
        val heightRatio = getHeightRatio(height)
        zoom = Math.max(0.05, Math.min(widthRatio, heightRatio))
    }

    /**
     * Calculates the ratio of the given width minus the insets to the total width.
     */
    private fun getWidthRatio(width: Int): Double {
        val totalInsetsWidth = (printInfo!!.columnCount * 2 * DESKTOP_INSETS).toDouble()
        val totalPaperWidth = printInfo!!.columnCount.toDouble() * pageFormat!!.width * PRINT_TO_SCREEN_DPI
        return (width - totalInsetsWidth) / totalPaperWidth
    }

    /**
     * Calculates the ratio of the given height minus the insets to the total height.
     */
    private fun getHeightRatio(height: Int): Double {
        val totalInsetsHeight = (printInfo!!.rowCount * 2 * DESKTOP_INSETS).toDouble()
        val totalPaperHeight = printInfo!!.rowCount.toDouble() * pageFormat!!.height * PRINT_TO_SCREEN_DPI
        return (height - totalInsetsHeight) / totalPaperHeight
    }

    /**
     * Force a recalculation and revalidate / repaint.
     */
    fun adjust() {
        val completePaperFrameWidth = 2 * DESKTOP_INSETS + pageFormat!!.width * this.zoom * PRINT_TO_SCREEN_DPI
        val completePaperFrameHeight = 2 * DESKTOP_INSETS + pageFormat!!.height * this.zoom * PRINT_TO_SCREEN_DPI
        preferredSize = Dimension(Math.rint(printInfo!!.columnCount * completePaperFrameWidth).toInt(),
                Math.rint(printInfo!!.rowCount * completePaperFrameHeight).toInt())
        revalidate()
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g2d = graphics.create() as Graphics2D
        // find out if the page format is landscape or portrait
        val paper = this.pageFormat!!.paper
        val isPortrait = this.pageFormat!!.orientation == PageFormat.PORTRAIT
        // the bounds for one paper
        var paperBounds = RectD(0.0, 0.0, if (isPortrait) paper.width else paper.height, if (isPortrait) paper.height else paper.width)
        // include the insets that are set for the JComponent
        val componentInsets = super.getInsets()
        // the real zoom
        val zoom = PRINT_TO_SCREEN_DPI * this.zoom
        // an awt shape instance that will be used for the drawing.
        val paperRect = Rectangle2D.Double(0.0, 0.0, 0.0, 0.0)

        try {
            // draw each paper separately
            for (row in 0 until this.printInfo!!.rowCount) {
                for (column in 0 until this.printInfo!!.columnCount) {

                    // find out the location of the paper in the grid, taking into account insets, spacing and so on
                    val insetWidthSoFar = (column * 2 * DESKTOP_INSETS + DESKTOP_INSETS).toDouble()
                    val insetHeightSoFar = (row * 2 * DESKTOP_INSETS + DESKTOP_INSETS).toDouble()
                    val paperWidthSoFar = column * (paperBounds.getWidth() * zoom)
                    val paperHeightSoFar = row * (paperBounds.getHeight() * zoom)

                    val deltaX = componentInsets.left.toDouble() + paperWidthSoFar + insetWidthSoFar
                    val deltaY = componentInsets.top.toDouble() + paperHeightSoFar + insetHeightSoFar

                    paperBounds = RectD(deltaX, deltaY, paperBounds.getWidth(), paperBounds.getHeight())

                    // first draw the frame before drawing the content
                    drawPaperFrame(g2d, paperBounds, zoom, paperRect)

                    val transX = paperRect.x
                    val transY = paperRect.y

                    // try draw the content of the paper using the CanvasPrintable on top of the paper frame we just created.
                    val result = drawPaperContent(g2d, paperBounds, zoom, row, column, transX, transY, paperRect)

                    if (result == Printable.NO_SUCH_PAGE) {
                        // draws a placeholder in case the page requested to be drawn doesn't exist
                        // actually doesn't happy when the rows and columns are not manipulated since the panel always calculates the exact amount of papers
                        // included anyways for demonstrational purposes
                        drawEmptyPaper(g2d, paperBounds, zoom, paperRect)
                    } else {
                        // else everything was alright and we can just draw the border of the imageable area and move on
                        paperRect.setFrame(
                                this.pageFormat!!.imageableX * zoom + transX,
                                this.pageFormat!!.imageableY * zoom + transY,
                                this.pageFormat!!.imageableWidth * zoom,
                                this.pageFormat!!.imageableHeight * zoom)
                        drawPrintableAreaBorder(g2d, paperRect)
                    }
                }
            }
        } finally {
            g2d.dispose()
        }
    }

    /**
     * Draws a "paper", which means a rectangle with white fill paint, black border and a drop shadow.
     * The content of the paper which will be on the print output is drawn later on top of it.
     * @param g2d the context to draw onto.
     * @param bounds the bounds in the grid of the paper to draw.
     * @param zoom the current onscreen zoom of the panel.
     * @param paperRect the shape to use for drawing.
     */
    private fun drawPaperFrame(g2d: Graphics2D, bounds: RectD, zoom: Double, paperRect: Rectangle2D.Double) {
        // draw drop shadow
        paperRect.x = bounds.getX() + DROP_SHADOW_OFFSET
        paperRect.y = bounds.getY() + DROP_SHADOW_OFFSET
        paperRect.width = bounds.getWidth() * zoom
        paperRect.height = bounds.getHeight() * zoom

        g2d.color = this.background.darker()
        g2d.fill(paperRect)

        // draw paper
        paperRect.x = bounds.getX()
        paperRect.y = bounds.getY()

        g2d.color = Color.white
        g2d.fill(paperRect)
        g2d.color = Color.black
        g2d.draw(paperRect)
    }

    /**
     * Draws the actual content of a paper that would be later printed on it.
     * @param g2d the context to draw onto.
     * @param bounds the bounds in the grid of the paper to draw.
     * @param zoom the current onscreen zoom of the panel.
     * @param row the index of the row of the paper to draw.
     * @param column the index of the column of the paper to draw.
     * @param transX the x offset of the content in paper coordinates.
     * @param transY the y offset of the content in paper coordinates.
     * @param paperRect the shape to use for drawing.
     */
    private fun drawPaperContent(g2d: Graphics2D, bounds: RectD, zoom: Double, row: Int, column: Int,
                                 transX: Double, transY: Double, paperRect: Rectangle2D.Double): Int {
        var result: Int
        try {
            val oldTrans = g2d.transform
            val oldClip = g2d.clip
            g2d.translate(transX, transY)
            g2d.scale(zoom, zoom) // zoom!
            val prevClip = g2d.clip

            paperRect.setFrame(
                    this.pageFormat!!.imageableX,
                    this.pageFormat!!.imageableY,
                    this.pageFormat!!.imageableWidth,
                    this.pageFormat!!.imageableHeight)

            val paperContents = Rectangle(0, 0, Math.ceil(bounds.getWidth()).toInt(), Math.ceil(bounds.getHeight()).toInt())
            result = if (prevClip.intersects(paperContents)) {
                g2d.clip(paperRect)
                this.printable.print(g2d, this.pageFormat, column + this.printInfo!!.columnCount * row)
            } else {
                Printable.PAGE_EXISTS
            }
            g2d.transform = oldTrans
            g2d.clip = oldClip
        } catch (pe: Exception) {
            result = Printable.NO_SUCH_PAGE
            pe.printStackTrace()
        }

        return result
    }

    /**
     * Draws the content for an "empty" paper, which means a paper for which the result of the printing was Printable.NO_SUCH_PAGE.
     * The way this panel and printable is build and maintained this shouldn't happen in this demo, but for completeness sake it is
     * included.
     * This draws a big red "X" across the paper on a gray background.
     * @param g2d the context to draw onto.
     * @param bounds the bounds in the grid of the paper to draw.
     * @param zoom the current onscreen zoom of the panel.
     * @param paperRect the shape to use for drawing.
     */
    private fun drawEmptyPaper(g2d: Graphics2D, bounds: RectD, zoom: Double, paperRect: Rectangle2D.Double) {

        paperRect.x = bounds.getX()
        paperRect.y = bounds.getY()
        paperRect.width = bounds.getWidth() * zoom
        paperRect.height = bounds.getHeight() * zoom

        // fill the rectangular paper area
        g2d.color = Color.lightGray
        g2d.fill(paperRect)

        // draw a red "X" across the paper
        val line = Line2D.Double(paperRect.x, paperRect.y, paperRect.x + paperRect.width, paperRect.y + paperRect.height)
        g2d.color = Color.red
        g2d.draw(line)
        line.x1 = line.x2
        line.x2 = paperRect.x
        g2d.draw(line)

        // draw the border
        g2d.color = Color.darkGray
        g2d.draw(paperRect)
    }

    /**
     * Draws the given rect in a light gray color. This is used to draw the border for the imageable area of the paper.
     * @param g2d the context to draw onto.
     * @param paperRect the shape to use for drawing.
     */
    private fun drawPrintableAreaBorder(g2d: Graphics2D, paperRect: Rectangle2D.Double) {
        g2d.color = Color.lightGray
        g2d.draw(paperRect)
    }

    /**
     * Updates the number of columns and rows using the CanvasPrintable.
     */
    fun updatePrintInfo() {
        this.printInfo = this.printable.createPrintInfo(pageFormat)
    }

    /*
    Scrollable specific methods that we implement trivially.
   */

    override fun getPreferredScrollableViewportSize(): Dimension {
        return preferredSize
    }

    override fun getScrollableBlockIncrement(rectangle: Rectangle, param: Int, param2: Int): Int {
        return 10
    }

    override fun getScrollableTracksViewportHeight(): Boolean {
        return false
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        return false
    }

    override fun getScrollableUnitIncrement(rectangle: Rectangle, param: Int, param2: Int): Int {
        return 20
    }

    companion object {
        // Some useful constants, for simplicity
        private const val DESKTOP_INSETS = 10
        private const val DROP_SHADOW_OFFSET = 5
        private const val PRINT_TO_SCREEN_DPI = 96.0 / 72.0
    }
}

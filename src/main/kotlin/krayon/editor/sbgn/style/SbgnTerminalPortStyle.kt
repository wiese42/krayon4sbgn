/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.sbgn.style

import com.yworks.yfiles.geometry.*
import com.yworks.yfiles.graph.*
import com.yworks.yfiles.graph.labelmodels.FreeNodeLabelModel
import com.yworks.yfiles.graph.styles.AbstractPortStyle
import com.yworks.yfiles.graph.styles.DefaultLabelStyle
import com.yworks.yfiles.utils.IEventListener
import com.yworks.yfiles.view.*
import com.yworks.yfiles.view.input.*
import krayon.editor.base.style.*
import krayon.editor.base.util.*
import krayon.editor.sbgn.model.SbgnPropertyKey
import krayon.editor.sbgn.model.getSbgnProperty
import krayon.editor.sbgn.model.setSbgnProperty

import java.awt.*
import java.awt.geom.*
import kotlin.math.abs

class SbgnTerminalPortStyle : AbstractPortStyle(), IStyleable {

    var pen: Pen = Pen.getBlack()
    var paint:Paint = Color.WHITE

    var font = Font("Dialog", Font.PLAIN, 10)
    @Suppress("MemberVisibilityCanBePrivate")
    var fontColor:Paint = Color.BLACK

    private var port:IPort? = null

    private var terminalSize:SizeD
        get() = port?.getSbgnProperty(SbgnPropertyKey.TERMINAL_SIZE) as? SizeD ?: SizeD(20.0,20.0)
        set(value) { port?.setSbgnProperty(SbgnPropertyKey.TERMINAL_SIZE, value) }

    private var text: String
        get() = port?.getSbgnProperty(SbgnPropertyKey.TERMINAL_LABEL) as? String ?: ""
        set(value) { port?.setSbgnProperty(SbgnPropertyKey.TERMINAL_LABEL, value) }

    private var orientation: String
        get() = port?.getSbgnProperty(SbgnPropertyKey.ORIENTATION) as String
        set(value) { port?.setSbgnProperty(SbgnPropertyKey.ORIENTATION, value) }

    private var bounds = MutableRectangle()
    private var tagStyle = SbgnTagStyle()

    private var prevOrientation:String? = null

    override fun clone(): AbstractPortStyle {
        val copy = super.clone() as SbgnTerminalPortStyle
        copy.bounds = bounds.clone()
        copy.tagStyle = tagStyle.clone() as SbgnTagStyle
        return copy
    }

    override fun applyStyle(context: IStyleableContext, map: Map<StyleProperty, Any?>) {
        (map[StyleProperty.FontSize] as? Double)?.let { font = font.deriveFont(it.toFloat()) }
        (map[StyleProperty.FontStyle] as? FontStyleValue)?.let { font = font.deriveFont(it.code) }
        (map[StyleProperty.TextColor] as? Color)?.let {  fontColor = it }
        (map[StyleProperty.OutlineColor] as? Color)?.let { pen = Pen(it, pen.thickness) }
        (map[StyleProperty.OutlineWidth] as? Double)?.let { pen = Pen(pen.paint, it) }
        (map[StyleProperty.Background] as? Paint)?.let { paint = it }
        (map[StyleProperty.Width] as? Double)?.let { terminalSize = terminalSize.withWidth(it) }
        (map[StyleProperty.Height] as? Double)?.let {
            terminalSize = terminalSize.withHeight(it) }
    }

    override fun retrieveStyle(context: IStyleableContext, map:MutableMap<StyleProperty, Any?>) {
        map[StyleProperty.FontSize] = font.size2D.toDouble()
        map[StyleProperty.FontStyle] = FontStyleValue.values().first { it.code == font.style }
        map[StyleProperty.TextColor] = fontColor as Color
        map[StyleProperty.OutlineColor] = pen.paint ?: Color(0,0,0,0)
        map[StyleProperty.OutlineWidth] = pen.thickness
        map[StyleProperty.Background] = paint
        map[StyleProperty.Width] = terminalSize.width
        map[StyleProperty.Height] = terminalSize.height
    }

    /**
     * Creates the visual for a port.
     */
    override fun createVisual(context: IRenderContext, port: IPort): IVisual {
        this.port = port
        return VisualGroup().apply {
            add(PortVisual().apply { update() })
            add(TextVisual(text, font, fontColor, bounds))
        }
    }

    /**
     * Re-renders the port using the old visual instead of creating a new one for each call. It is strongly recommended to
     * do it for performance reasons. Otherwise, [.createVisual] is called instead.
     */
    override fun updateVisual(context: IRenderContext, group: IVisual, port: IPort): IVisual {
        this.port = port
        val portVisual = (group as VisualGroup).children[0] as PortVisual
        val textVisual = group.children[1] as TextVisual
        portVisual.update()
        textVisual.update(text, font, fontColor, bounds)
        return group
    }

    /**
     * Calculates the bounds of this port.
     * These are also used for arranging the visual, hit testing, visibility testing, and marquee box tests.
     */
    override fun getBounds(context: ICanvasContext, port: IPort): RectD {
        this.port = port
        //println("getBounds ${(port.style as SbgnTerminalPortStyle).text}")
        updateFeatures(port)
        return bounds.toRectD()
    }

    private fun updateTerminalSize(o1:String, o2: String?) {
        val hasFlipped = when (o1) {
            "left", "right" -> o2 == "up" || o2 == "down"
            "up", "down" -> o2 == "left" || o2 == "right"
            else -> false
        }
        if(hasFlipped) {
            //println("hasFlipped from $o2 to $o1")
            terminalSize = SizeD(terminalSize.height, terminalSize.width)
        }
    }

    private fun updateFeatures(port:IPort) {
        val portPoint = port.location
        val nodeRect = (port.owner as INode).layout
        when {
            abs(portPoint.x - nodeRect.x) < 0.1 -> {
                orientation = "right"
                updateTerminalSize("right", prevOrientation)
                bounds.setBounds(portPoint.x, portPoint.y-terminalSize.height*0.5, terminalSize.width, terminalSize.height)
            }
            abs(portPoint.x - nodeRect.maxX) < 0.1 -> {
                orientation = "left"
                updateTerminalSize("left", prevOrientation)
                bounds.setBounds(portPoint.x-terminalSize.width, portPoint.y-terminalSize.height*0.5, terminalSize.width, terminalSize.height)

            }
            abs(portPoint.y - nodeRect.y) < 0.1 -> {
                orientation = "down"
                updateTerminalSize("down", prevOrientation)
                bounds.setBounds(portPoint.x-terminalSize.width*0.5, portPoint.y, terminalSize.width, terminalSize.height)
            }
            abs(portPoint.y - nodeRect.maxY) < 0.1 -> {
                orientation = "up"
                updateTerminalSize("up", prevOrientation)
                bounds.setBounds(portPoint.x-terminalSize.width*0.5, portPoint.y-terminalSize.height, terminalSize.width, terminalSize.height)
            }
            else -> bounds.setBounds(portPoint.x, portPoint.y, terminalSize.width, terminalSize.height)
        }

        prevOrientation = orientation
    }

    private inner class PortVisual : IVisual {

        private var shape: Path2D? = null
        val dummyNode = SimpleNode()

        fun update() {
            dummyNode.setSbgnProperty(SbgnPropertyKey.ORIENTATION, orientation)
            shape = tagStyle.createGeneralPath(dummyNode, bounds.toSizeD()).createPath(Matrix2D().apply { translate(bounds.toPointD()) })
        }

        override fun paint(context: IRenderContext, g: Graphics2D) {
            val oldPaint = g.paint
            val oldStroke = g.stroke
            g.paint = paint
            g.fill(shape)
            pen.adopt(g)
            g.draw(shape)
            g.paint = oldPaint
            g.stroke = oldStroke
        }
    }

    override fun lookup(port: IPort, type: Class<*>): Any? {
        return when(type) {
            ISelectionIndicatorInstaller::class.java -> {
                RectangleIndicatorInstaller(bounds)
            }

            IHandleProvider::class.java -> {
                object:PortsHandleProvider(port.owner) {
                    override fun getHandle(port: IPort): IHandle {
                        //abuse ConstraintHandle as decorator
                        val style = port.style as SbgnTerminalPortStyle
                        return object:ConstrainedHandle(super.getHandle(port)) {
                            override fun getLocation() = style.bounds.center
                            override fun getType() = HandleTypes.MOVE
                            override fun getCursor() = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                            override fun constrainNewLocation(context: IInputModeContext, origP: PointD, newP: PointD) = newP
                            override fun onFinished(context: IInputModeContext, p1: PointD, p2: PointD) {
                                context.graph.addValueUndoEdit("", null, null, { updateFeatures(port) })
                            }
                        }
                    }
                }
            }

            IReshapeHandleProvider::class.java -> {
                object:RectangleReshapeHandleProvider(object:IMutableRectangle {
                    override fun getY() = bounds.y
                    override fun setX(x: Double) {}
                    override fun getHeight() = bounds.height
                    override fun getX() = bounds.x
                    override fun setY(y: Double) {}
                    override fun getWidth() = bounds.width

                    override fun setWidth(width: Double) {
                        terminalSize = terminalSize.withWidth(width)
                        updateFeatures(port)
                    }

                    override fun setHeight(height: Double) {
                        terminalSize = terminalSize.withHeight(height)
                        updateFeatures(port)
                    }
                }, HandlePositions.BORDER) {
                    override fun getHandle(context: IInputModeContext, position: HandlePositions): IHandle {
                        return object:ConstrainedHandle(super.getHandle(context, position)) {
                            lateinit var startSize:SizeD
                            override fun constrainNewLocation(context: IInputModeContext, origP: PointD, newP: PointD): PointD {
                                val nodeBox = (port.owner as INode).layout
                                return if(origP.isOnBorder(nodeBox)) origP else newP
                            }

                            override fun onInitialized(context: IInputModeContext?, p1: PointD) {
                                startSize = terminalSize
                            }
                            override fun onFinished(context: IInputModeContext, p1: PointD, p2: PointD) {
                                context.graph.addValueUndoEdit("Change Terminal Size", startSize, terminalSize, { terminalSize = it })
                            }
                        }
                    }
                }
            }
            else -> super.lookup(port, type)
        }
    }

    companion object {
        fun createEditLabelOnClickListener(geim:GraphEditorInputMode): IEventListener<ItemClickedEventArgs<IModelItem>> {
            return object:IEventListener<ItemClickedEventArgs<IModelItem>> {
                var labelChangedListener:IEventListener<LabelEventArgs>? = null
                var node: SimpleNode = SimpleNode()
                var label:SimpleLabel = SimpleLabel(node,"",FreeNodeLabelModel.INSTANCE.createDefaultParameter()).apply {
                    style = DefaultLabelStyle().apply {
                        textAlignment = TextAlignment.CENTER; verticalTextAlignment = VerticalAlignment.CENTER
                    }
                }
                var style: SbgnTerminalPortStyle? = null

                override fun onEvent(any: Any, args: ItemClickedEventArgs<IModelItem>) {
                    val port = args.item
                    if(port is IPort && port.style is SbgnTerminalPortStyle) {
                        args.isHandled = true
                        style = port.style as SbgnTerminalPortStyle
                        val bounds = style?.bounds?.toRectD()
                        node.layout = bounds
                        label.layoutParameter = FreeNodeLabelModel.INSTANCE.createCanonicalParameter(bounds, OrientedRectangle(bounds))
                        label.text = style?.text
                        label.preferredSize = bounds?.toSizeD()

                        if(labelChangedListener == null) {
                            labelChangedListener = IEventListener { _, labelArgs ->
                                if(labelArgs.item === label) {
                                    geim.graph.addValueUndoEdit("Change Terminal Text", style!!.text, labelArgs.item.text, { style?.text = it })
                                    style?.text = labelArgs.item.text
                                }
                            }
                            geim.addLabelTextChangedListener(labelChangedListener)
                        }
                        geim.editLabel(label)
                    }
                }
            }
        }
    }
}

/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.util

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.geometry.RectD
import com.yworks.yfiles.view.*
import krayon.editor.base.style.ShapeVisualWithBounds
import krayon.editor.base.style.TextVisual
import java.awt.Color
import java.awt.Font
import java.awt.Paint
import java.awt.geom.RoundRectangle2D
import javax.swing.Timer

class EventMonitor(private val graphComponent: GraphComponent, private val externalEventSource: IEventSource? = null) {

    data class EventDescription(val displayText:String)
    interface IEventSource {
        fun addCallback(callback:(EventDescription) -> Unit)
    }

    private fun onExternalEvent(description: EventDescription) {
        updateVisual(description.displayText)
        clearVisual()
    }

    var textColor = Color.BLACK!!
    var background: Paint? = Color.WHITE
    var pen:Pen? = Pen.getBlack()

    val displayVisual = VisualGroup().apply {
        add(ShapeVisualWithBounds(RoundRectangle2D.Double(), pen, background))
        add(TextVisual("", Font("Dialog",Font.PLAIN, 30),textColor, RectD.EMPTY).apply { horizontalAlignment = TextVisual.HorizontalAlignment.LEFT })
    }
    val emptyVisual = VisualGroup()

    private var displayText:String = ""
    var isRunning = false
        private set
    private var isInitialized = false


    fun start() {
        if(!isRunning && !isInitialized) initialize()
        isRunning = true
    }

    fun stop() {
        isRunning = false
    }

    private fun initialize() {
        isInitialized = true
        externalEventSource?.addCallback(::onExternalEvent)

        with(graphComponent) {
            addMouse2DReleasedListener { _, _ ->
                clearVisual()
            }
            addMouse2DDraggedListener { _, args ->
                val text = "${args.modifiers.toDisplayString()}${args.buttons.toDisplayString()}Mouse Drag"
                updateVisual(text)
            }
            addMouse2DWheelTurnedListener { _, args ->
                val dir = if(args.wheelDelta > 0) "Up" else "Down"
                val text = "Mouse Wheel $dir"
                updateVisual(text)
            }
            addMouse2DClickedListener { _, args ->
                val op = if(args.clickCount > 1) "Double-Click" else "Click"
                val text = "${args.modifiers.toDisplayString()}Mouse $op"
                updateVisual(text)
            }
            graphComponent.rootGroup.addChild(visualCreator, ICanvasObjectDescriptor.ALWAYS_DIRTY_INSTANCE)
        }
    }

    private val visualCreator = object:IVisualCreator {
        var lastZoom = 0.0
        var lastViewPoint = PointD.ORIGIN
        override fun createVisual(context: IRenderContext): IVisual {
            return updateVisual(context, emptyVisual)
        }

        override fun updateVisual(context: IRenderContext, group: IVisual): IVisual {
            return if (!isRunning || displayText.isEmpty()) emptyVisual
            else with(graphComponent.bounds) {
                val shapeVisual = displayVisual.children[0] as ShapeVisualWithBounds
                shapeVisual.paint = background
                shapeVisual.pen = pen
                val textVisual = displayVisual.children[1] as TextVisual
                textVisual.textColor = textColor
                if(textVisual.text != displayText || lastZoom != context.zoom || lastViewPoint != context.canvasComponent.viewPoint) {
                    lastZoom = context.zoom
                    lastViewPoint = context.canvasComponent.viewPoint
                    displayVisual.transform = context.toViewTransform
                    textVisual.text = displayText
                    val size = TextVisual.calculatePreferredSize(displayText, textVisual.font)
                    val bounds = RectD.fromCenter(PointD(centerX, maxY-size.height-60.0), size)
                    textVisual.layout = bounds
                    (shapeVisual.shape as RoundRectangle2D).setRoundRect(bounds.x-20.0, bounds.y-20.0, bounds.width+40.0, bounds.height+40.0, 20.0, 20.0)
                }


                displayVisual
            }
        }
    }

    val timer = Timer(1000, {
        updateVisual("")
    })

    //fun mouseLocationToText(mouseLocation:PointD) =  "[${mouseLocation.x.toInt()}, ${mouseLocation.y.toInt()}]"
    private fun ModifierKeys.toDisplayString():String {
        val keyModifierList = mutableListOf<String>()
        if(contains(ModifierKeys.ALT)) keyModifierList += "Alt "
        if(contains(ModifierKeys.CONTROL)) keyModifierList += "Ctrl "
        if(contains(ModifierKeys.SHIFT)) keyModifierList += "Shift "
        return keyModifierList.joinToString("+")
    }

    private fun MouseButtons.toDisplayString() = when(this) {
        MouseButtons.LEFT -> "Left "
        MouseButtons.MIDDLE -> "Middle "
        MouseButtons.RIGHT -> "Right "
        else -> "???"
    }

    private fun updateVisual(text:String) {
       if(text != displayText) {
           displayText = text
           graphComponent.repaint()
       }
        if(text == "") timer.stop()
    }

    private fun clearVisual() {
        timer.restart()
    }
}
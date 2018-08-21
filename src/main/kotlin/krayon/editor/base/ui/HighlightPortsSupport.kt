/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.graph.INode
import com.yworks.yfiles.graph.IPort
import com.yworks.yfiles.graph.SimplePort
import com.yworks.yfiles.graph.portlocationmodels.FreeNodePortLocationModel
import com.yworks.yfiles.view.DefaultPortCandidateDescriptor
import com.yworks.yfiles.view.ICanvasObject
import com.yworks.yfiles.view.input.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class HighlightPortsSupport {

    var node: INode? = null
    var focusedPortCandidate:IPortCandidate? = null
    var isModifierDown = false
    var inputModeContext: IInputModeContext? = null

    private var canvasObjects = ArrayList<ICanvasObject>()

    private val keyListener = object: KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            updateModifierState(e)
        }

        override fun keyReleased(e: KeyEvent) {
            updateModifierState(e)
        }

        fun updateModifierState(e: KeyEvent) {
            if(e.isShiftDown != isModifierDown && node != null) {
                isModifierDown = e.isShiftDown
                updatePortHighlights(node!!)
            }
        }
    }

    private fun getResolvedPortCandidates(location: PointD, candidates: Iterable<IPortCandidate>):Iterable<IPortCandidate> {
        return candidates.map {
            if(it.validity == PortCandidateValidity.DYNAMIC) it.getPortCandidateAt(inputModeContext,location)
            else it
        }
    }

    companion object {
        fun getClosestPortCandidate(node:INode, location: PointD, candidates:Iterable<IPortCandidate>?): IPortCandidate? {
            var bestDist = Double.MAX_VALUE
            var bestPortCandidate: IPortCandidate? = null
            val dummyPort:IPort = SimplePort(node, FreeNodePortLocationModel.INSTANCE.createParameter(PointD.ORIGIN))
            candidates?.forEach { candidate ->
                val p = candidate.locationParameter.model.getLocation(dummyPort, candidate.locationParameter)
                val dist = p.distanceTo(location)
                if(dist < bestDist && candidate.validity != PortCandidateValidity.INVALID) {
                    bestDist = dist
                    bestPortCandidate = candidate
                }
            }
            return bestPortCandidate
        }
    }

    @Suppress("MoveLambdaOutsideParentheses")
    fun installPortHighlights(node: INode, location:PointD? = null, resolveDynamicPorts: Boolean? = null, spcValidator:((IPortCandidate) -> Boolean)? = null) {
        //println("install")
        this.node = node

        var pcp = node.lookup(IPortCandidateProvider::class.java)
        if(pcp  != null) {
            if(spcValidator != null) {
                pcp = ValidatingPortCandidateProvider(pcp, spcValidator, {true})
            }

            val mouseLocation = location ?: inputModeContext!!.canvasComponent.lastEventLocation
            val candidates = pcp.getSourcePortCandidates(inputModeContext).let {
                if(resolveDynamicPorts ?: isModifierDown) getResolvedPortCandidates(mouseLocation, it) else it
            }

            val closestCandidate = getClosestPortCandidate(node, mouseLocation, candidates)
            val normalDescriptor = DefaultPortCandidateDescriptor()
            val closestDescriptor = DefaultPortCandidateDescriptor().apply {
                isCurrentCandidate = true
            }
            candidates.forEach { candidate ->

                val descriptor = if(candidate === closestCandidate) closestDescriptor else normalDescriptor
                canvasObjects.add(inputModeContext!!.canvasComponent.inputModeGroup.addChild(candidate,descriptor))
            }

            focusedPortCandidate = closestCandidate
        }
    }

    fun deinstallPortHighlights() {
        node = null
        focusedPortCandidate = null
        isModifierDown = false
        if(canvasObjects.size > 0) {
            //println("deinstall")
            canvasObjects.forEach{ it.remove() }
            canvasObjects.clear()
        }
    }

    fun updatePortHighlights(node: INode?, location: PointD? = null, resolveDynamicPorts:Boolean? = null) {
        deinstallPortHighlights()
        if(node != null) installPortHighlights(node, location, resolveDynamicPorts)
    }

    fun install(context: IInputModeContext) {
        inputModeContext = context
        context.canvasComponent.addKeyListener(keyListener)
    }

    fun uninstall(context: IInputModeContext) {
        context.canvasComponent.removeKeyListener(keyListener)
        inputModeContext = null
        node = null
        deinstallPortHighlights()
    }

}
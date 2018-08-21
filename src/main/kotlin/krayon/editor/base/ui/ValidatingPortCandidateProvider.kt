/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.view.input.*

class ValidatingPortCandidateProvider(private val delegateProvider: IPortCandidateProvider, private val validSpc:(IPortCandidate) -> Boolean, private val validTpc: (IPortCandidate) -> Boolean): IPortCandidateProvider {

    override fun getTargetPortCandidates(context: IInputModeContext, spc: IPortCandidate): MutableIterable<IPortCandidate> {
        return delegateProvider.getTargetPortCandidates(context,spc).map { validityMapper(it, validTpc)}. toMutableList()
    }

    override fun getTargetPortCandidates(context: IInputModeContext): MutableIterable<IPortCandidate> {
        return delegateProvider.getTargetPortCandidates(context).map { validityMapper(it, validTpc)}. toMutableList()
    }

    override fun getSourcePortCandidates(context: IInputModeContext, tpc: IPortCandidate?): MutableIterable<IPortCandidate> {
        return delegateProvider.getSourcePortCandidates(context,tpc).map { validityMapper(it, validSpc)}. toMutableList()
    }

    override fun getSourcePortCandidates(context: IInputModeContext): MutableIterable<IPortCandidate> {
        return delegateProvider.getSourcePortCandidates(context).map { validityMapper(it, validSpc)}. toMutableList()
    }

    private fun validityMapper(pc: IPortCandidate, test:(IPortCandidate) -> Boolean): IPortCandidate {
        return when {
            test(pc) -> pc
            pc.port != null -> DefaultPortCandidate(pc.port).apply { validity = PortCandidateValidity.INVALID }
            else -> DefaultPortCandidate(pc.owner, pc.locationParameter, PortCandidateValidity.INVALID)
        }
    }
}
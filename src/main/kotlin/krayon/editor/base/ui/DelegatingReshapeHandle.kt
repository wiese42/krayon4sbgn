/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import com.yworks.yfiles.geometry.IPoint
import com.yworks.yfiles.geometry.PointD
import com.yworks.yfiles.view.input.HandleTypes
import com.yworks.yfiles.view.input.IHandle
import com.yworks.yfiles.view.input.IInputModeContext
import java.awt.Cursor

open class DelegatingReshapeHandle(private val delegate: IHandle) : IHandle {
    override fun handleMove(context: IInputModeContext, p1: PointD?, p2: PointD?) {
        delegate.handleMove(context,p1,p2)
    }

    override fun getLocation(): IPoint {
        return delegate.location
    }

    override fun initializeDrag(context: IInputModeContext) {
        delegate.initializeDrag(context)
    }

    override fun dragFinished(context: IInputModeContext, p1: PointD, p2: PointD) {
        delegate.dragFinished(context,p1,p2)
    }

    override fun cancelDrag(context: IInputModeContext, p1: PointD) {
        delegate.cancelDrag(context, p1)
    }

    override fun getType(): HandleTypes {
        return delegate.type
    }

    override fun getCursor(): Cursor {
        return delegate.cursor
    }
}
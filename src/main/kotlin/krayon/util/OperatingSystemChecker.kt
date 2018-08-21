/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.util

object OperatingSystemChecker {

    private val OS by lazy {
        System.getProperty("os.name").toLowerCase()
    } 
    
    @Suppress("unused")
    val isWindows: Boolean
        get() = OS.indexOf("win") >= 0

    @Suppress("unused")
    val isMac: Boolean
        get() = OS.indexOf("mac") >= 0

    @Suppress("unused")
    val isUnix: Boolean
        get() =
            OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0

    @Suppress("unused")
    val isSolaris: Boolean
        get() = OS.indexOf("sunos") >= 0

}
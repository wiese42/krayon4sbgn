
/*
 * Copyright (c) 2018 Roland Wiese
 * This software is licensed under the Apache License, Version 2.0 (the "License"); you may not use this software except
 * in compliance with the License. You may obtain a copy of the License at  http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package krayon.editor.base.ui

import javafx.application.Platform
import javafx.stage.FileChooser
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.function.Function
import java.util.function.Supplier

@Suppress("unused", "MemberVisibilityCanBePrivate")
/**
 * A utility class that summons JavaFX FileChooser from the Swing EDT.
 * (Or anywhere else for that matter.) JavaFX should be initialized prior to
 * using this class (e. g. by creating a JFXPanel instance). It is also
 * recommended to call Platform.setImplicitExit(false) after initialization
 * to ensure that JavaFX platform keeps running. Don't forget to call
 * Platform.exit() when shutting down the application, to ensure that
 * the JavaFX threads don't prevent JVM exit.
 */
class SynchronousJFXFileChooser
/**
 * Constructs a new file fileChooser that will use the provided factory.
 *
 * The factory is accessed from the JavaFX event thread, so it should either
 * be immutable or at least its state shouldn't be changed randomly while
 * one of the dialog-showing method calls is in progress.
 *
 * The factory should create and set up the fileChooser, for example,
 * by setting extension filters. If there is no need to perform custom
 * initialization of the fileChooser, FileChooser::new could be passed as
 * a factory.
 *
 * Alternatively, the method parameter supplied to the showDialog()
 * function can be used to provide custom initialization.
 *
 * @param fileChooserFactory the function used to construct new choosers
 */
(private val fileChooserFactory: Supplier<FileChooser>) {

    /**
     * Shows the FileChooser dialog by calling the provided method.
     *
     * Waits for one second for the dialog-showing task to start in the JavaFX
     * event thread, then throws an IllegalStateException if it didn't start.
     *
     * @see .showDialog
     * @param <T> the return type of the method, usually File or List&lt;File&gt;
     * @param method a function calling one of the dialog-showing methods
     * @return whatever the method returns
    </T> */
    fun <T> showDialog(method: java.util.function.Function<FileChooser, T>): T? {
        return showDialog(method, 1, TimeUnit.SECONDS)
    }

    /**
     * Shows the FileChooser dialog by calling the provided method. The dialog
     * is created by the factory supplied to the constructor, then it is shown
     * by calling the provided method on it, then the result is returned.
     *
     *
     * Everything happens in the right threads thanks to
     * SynchronousJFXCaller. The task performed in the JavaFX thread
     * consists of two steps: construct a fileChooser using the provided factory
     * and invoke the provided method on it. Any exception thrown during these
     * steps will be rethrown in the calling thread, which shouldn't
     * normally happen unless the factory throws an unchecked exception.
     *
     *
     *
     * If the calling thread is interrupted during either the wait for
     * the task to start or for its result, then null is returned and
     * the Thread interrupted status is set.
     *
     * @param <T> return type (usually File or List&lt;File&gt;)
     * @param method a function that calls the desired FileChooser method
     * @param timeout time to wait for Platform.runLater() to *start*
     * the dialog-showing task (once started, it is allowed to run as long
     * as needed)
     * @param unit the time unit of the timeout argument
     * @return whatever the method returns
     * @throws IllegalStateException if Platform.runLater() fails to start
     * the dialog-showing task within the given timeout
    </T> */
    fun <T> showDialog(method: java.util.function.Function<FileChooser, T>,  timeout: Long, unit: TimeUnit): T? {
        val task = Callable {
            val chooser = fileChooserFactory.get()
            method.apply(chooser)
        }
        val caller = SynchronousJFXCallerK(task)
        return try {
            caller.call(timeout, unit)
        } catch (ex: RuntimeException) {
            throw ex
        } catch (ex: Error) {
            throw ex
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (ex: Exception) {
            throw AssertionError("Got unexpected checked exception from" + " SynchronousJFXCallerK.call()", ex)
        }

    }

    /**
     * Shows a FileChooser using FileChooser.showOpenDialog().
     *
     * @see .showDialog
     * @return the return value of FileChooser.showOpenDialog()
     */
    fun showOpenDialog(): File? {
        return showDialog<File>(Function({ chooser -> chooser.showOpenDialog(null) }))
    }

    /**
     * Shows a FileChooser using FileChooser.showSaveDialog().
     *
     * @see .showDialog
     * @return the return value of FileChooser.showSaveDialog()
     */
    fun showSaveDialog(): File? {
        return showDialog<File>(Function({ chooser -> chooser.showSaveDialog(null) }))
    }

    /**
     * Shows a FileChooser using FileChooser.showOpenMultipleDialog().
     *
     * @see .showDialog
     * @return the return value of FileChooser.showOpenMultipleDialog()
     */
    fun showOpenMultipleDialog(): List<File>? {
        return showDialog<List<File>>(Function({ chooser -> chooser.showOpenMultipleDialog(null) }))
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            @Suppress("UNUSED_VARIABLE")
            val dummy = javafx.embed.swing.JFXPanel()
            Platform.setImplicitExit(false)
            try {
                val chooser = SynchronousJFXFileChooser(Supplier {
                    val ch = FileChooser()
                    ch.title = "Open any file you wish"
                    ch
                })
                val file = chooser.showOpenDialog()
                println(file)
                // this will throw an exception:
                chooser.showDialog<File>(Function { ch -> ch.showOpenDialog(null) }, 1, TimeUnit.NANOSECONDS)
            } finally {
                Platform.exit()
            }
        }
    }

}
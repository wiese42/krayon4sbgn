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

import javax.swing.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A utility class to execute a Callable synchronously
 * on the JavaFX event thread.
 *
 * @param <T> the return type of the callable
</T> */
class SynchronousJFXCallerK<T>
/**
 * Constructs a new caller that will execute the provided callable.
 *
 * The callable is accessed from the JavaFX event thread, so it should either
 * be immutable or at least its state shouldn't be changed randomly while
 * the call() method is in progress.
 *
 * @param callable the action to execute on the JFX event thread
 */
(private val callable: Callable<T>) {

    /**
     * Executes the Callable.
     *
     *
     * A specialized task is run using Platform.runLater(). The calling thread
     * then waits first for the task to start, then for it to return a result.
     * Any exception thrown by the Callable will be rethrown in the calling
     * thread.
     *
     * @param startTimeout time to wait for Platform.runLater() to *start*
     * the dialog-showing task
     * @param startTimeoutUnit the time unit of the startTimeout argument
     * @return whatever the Callable returns
     * @throws IllegalStateException if Platform.runLater() fails to start
     * the task within the given timeout
     * @throws InterruptedException if the calling (this) thread is interrupted
     * while waiting for the task to start or to get its result (note that the
     * task will still run anyway and its result will be ignored)
     */
    @Throws(Exception::class)
    fun call(startTimeout: Long, startTimeoutUnit: TimeUnit): T? {
        val taskStarted = CountDownLatch(1)
        // Can't use volatile boolean here because only finals can be accessed
        // from closures like the lambda expression below.
        val taskCancelled = AtomicBoolean(false)
        // a trick to emulate modality:
        val modalBlocker = JDialog()
        modalBlocker.isModal = true
        modalBlocker.isUndecorated = true
        try {
            modalBlocker.opacity = 0.0f
        } catch(ex: Exception) {
            //ignore: does not work on some linux machines. KDE desktop?
        }
        modalBlocker.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        val modalityLatch = CountDownLatch(1)
        val task = FutureTask<T> {
            synchronized(taskStarted) {
                if (taskCancelled.get()) {
                    return@FutureTask null
                } else {
                    taskStarted.countDown()
                }
            }
            try {
                return@FutureTask callable.call()
            } finally {
                // Wait until the Swing thread is blocked in setVisible():
                modalityLatch.await()
                // and unblock it:
                SwingUtilities.invokeLater { modalBlocker.isVisible = false }
            }
        }
        Platform.runLater(task)
        if (!taskStarted.await(startTimeout, startTimeoutUnit)) {
            synchronized(taskStarted) {
                // the last chance, it could have been started just now
                if (!taskStarted.await(0, TimeUnit.MILLISECONDS)) {
                    // Can't use task.cancel() here because it would
                    // interrupt the JavaFX thread, which we don't own.
                    taskCancelled.set(true)
                    throw IllegalStateException("JavaFX was shut down" + " or is unresponsive")
                }
            }
        }
        // a trick to notify the task AFTER we have been blocked
        // in setVisible()
        SwingUtilities.invokeLater {
            // notify that we are ready to get the result:
            modalityLatch.countDown()
        }
        modalBlocker.isVisible = true // blocks
        modalBlocker.dispose() // release resources
        try {
            return task.get()
        } catch (ex: ExecutionException) {
            val ec = ex.cause
            if (ec is Exception) {
                throw ec
            } else if (ec is Error) {
                throw ec
            } else {
                throw AssertionError("Unexpected exception type", ec)
            }
        }

    }

}
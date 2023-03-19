package org.openrndr.extra.filewatcher

import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.launch
import java.io.File
import kotlin.reflect.KProperty

/**
 * Property delegator that watches a file. Changes are propagated right before the [Program] updates its extensions
 * @param program the program to synchronise updates with
 * @param file the file to watch
 * @param valueChangedEvent the event that is triggered when the value (after transformation) has changed
 * @param requestStopEvent an event that can be triggered to request the watcher to stop
 * @since 0.4.3
 * @see watchingFile
 */
class FileWatcherDelegate<T>(
    program: Program,
    file: File,
    valueChangedEvent: Event<T>? = null,
    requestStopEvent: Event<Unit>? = null,
    transducer: (File) -> T
) {
    private val watchValue = watchFile(file, valueChangedEvent, requestStopEvent, transducer)
    private var value = watchValue()

    init {
        // make sure that `value` is updated at the beginning of a draw cycle and not mid-cycle.
        program.launch {
            while (true) {
                value = watchValue()
                yield()
            }
        }
    }

    /**
     * Return transformed value
     */
    operator fun getValue(any: Any?, property: KProperty<*>): T {
        return value
    }
}

/**
 * Delegate value to a file watcher
 * @param file the file to watch
 * @param valueChangedEvent the event that is triggered when the value (after transformation) has changed
 * @param requestStopEvent an event that can be triggered to request the watcher to stop
 * @param transducer a function that transforms a [File] into a value of type [R]
 * @since 0.4.3
 * @see FileWatcherDelegate
 */
fun <R> Program.watchingFile(
    file: File,
    valueChangedEvent: Event<R>? = null,
    requestStopEvent: Event<Unit>? = null,
    transducer: (File) -> R
) = FileWatcherDelegate(this, file, valueChangedEvent, requestStopEvent, transducer)

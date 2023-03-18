import kotlinx.coroutines.yield
import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.extra.filewatcher.watchFile
import org.openrndr.launch
import java.io.File
import kotlin.reflect.KProperty

class FileWatcherDelegate<T>(
    program: Program,
    file: File,
    valueChangedEvent: Event<T>? = null,
    requestStopEvent: Event<Unit>? = null,
    transducer: (File) -> T
) {
    val watchValue = watchFile(file, valueChangedEvent, requestStopEvent, transducer)
    var value = watchValue()

    init {
        program.launch {
            while (true) {
                value = watchValue()
                yield()
            }
        }
    }

    operator fun getValue(any: Any, property: KProperty<*>): T {
        return value
    }
}

fun <R> Program.watchingFile(
    file: File,
    valueChangedEvent: Event<R>? = null,
    requestStopEvent: Event<Unit>? = null,
    transducer: (File) -> R
) = FileWatcherDelegate(this, file, valueChangedEvent, requestStopEvent, transducer)

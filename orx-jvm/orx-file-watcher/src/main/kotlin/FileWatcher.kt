package org.openrndr.extra.filewatcher

import com.sun.nio.file.SensitivityWatchEventModifier
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.util.WeakHashMap
import kotlin.concurrent.thread
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

private val watching = mutableMapOf<Path, MutableList<FileWatcher>>()
private val pathKeys = mutableMapOf<Path, WatchKey>()
private val keyPaths = WeakHashMap<WatchKey, Path>()
private val waiting = mutableMapOf<Path, Job>()

private val watchService by lazy {
    FileSystems.getDefault().newWatchService()
}

@OptIn(DelicateCoroutinesApi::class)
private val watchThread by lazy {
    thread(isDaemon = true) {
        while (true) {
            val key = watchService.take()
            val path = keyPaths[key]

            key.pollEvents().forEach {
                val contextPath = it.context() as Path
                val fullPath = path?.resolve(contextPath)

                fullPath?.let {
                    waiting[fullPath]?.cancel()
                    waiting[fullPath] = GlobalScope.launch {
                        delay(100)
                        watching[fullPath]?.forEach { w ->
                            w.triggerChange()
                        }
                    }
                }
            }
            key.reset()
        }
    }
}


class FileWatcher(
    val file: File,
    private val fileChangedEvent: Event<File>,
    requestStopEvent: Event<Unit>? = null
) {
    val path = file.absoluteFile.toPath()
    val parent = path.parent
    val key = pathKeys.getOrPut(parent) {
        parent.register(
            watchService, arrayOf(StandardWatchEventKinds.ENTRY_MODIFY),
            SensitivityWatchEventModifier.HIGH
        )
    }

    init {
        watchThread
        watching.getOrPut(path) {
            mutableListOf()
        }.add(this)
        keyPaths.getOrPut(key) { parent }
        requestStopEvent?.listenOnce {
            stop()
        }
    }

    fun stop() {
        synchronized(watching) {
            logger.info { "stopping, watcher stop requested" }
            watching[path]?.remove(this)
        }
    }

    internal fun triggerChange() {
        fileChangedEvent.trigger(file)
    }
}



fun <T> watchFile(
    file: File,
    contentsChangedEvent: Event<T>? = null,
    requestStopEvent: Event<Unit>? = null,
    transducer: (File) -> T
): () -> T {
    var result = transducer(file)
    val fileChangedEvent = Event<File>()
    val watcher = FileWatcher(file, fileChangedEvent, requestStopEvent)

    fileChangedEvent.listen {
        try {
            result = transducer(file)
            contentsChangedEvent?.trigger(result)
        } catch (e: Throwable) {
            logger.error(e) {
                """exception while transducing file"""
            }
        }
    }
    return {
        result
    }
}


//@JvmName("programWatchFile")
//fun <T> Program.watchFile(file: File, onChange: Event<T>? = null, transducer: (File) -> T): () -> T =
//    watchFile(this, file, onChange, transducer = transducer)

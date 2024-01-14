package org.openrndr.extra.filewatcher

import com.sun.nio.file.SensitivityWatchEventModifier
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.openrndr.events.Event
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.util.WeakHashMap
import kotlin.concurrent.thread

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

/**
 * @property file
 * @property fileChangedEvent
 * @param requestStopEvent
 */
class FileWatcher(
    private val file: File,
    private val fileChangedEvent: Event<File>,
    requestStopEvent: Event<Unit>? = null
) {
    private val path = file.absoluteFile.toPath()
    private val parent = path.parent
    private val key = pathKeys.getOrPut(parent) {
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

    @Suppress("MemberVisibilityCanBePrivate")
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

/**
 * Watch a file for changes
 * @param file the file to watch
 * @param valueChangedEvent the event that is triggered when the value (after transforming) has changed
 * @param requestStopEvent an event that can be triggered to request the watcher to stop
 * @param transducer a function that transforms a [File] into a value of type [R]
 */
fun <R> watchFile(
    file: File,
    valueChangedEvent: Event<R>? = null,
    requestStopEvent: Event<Unit>? = null,
    transducer: (File) -> R
): () -> R {
    var result = transducer(file)
    val fileChangedEvent = Event<File>()

    @Suppress("UNUSED_VARIABLE") val watcher = FileWatcher(file, fileChangedEvent, requestStopEvent)

    fileChangedEvent.listen {
        @Suppress("MemberVisibilityCanBePrivate")
        try {
            result = transducer(file)
            valueChangedEvent?.trigger(result)
        } catch (e: Throwable) {
            logger.error(e) {
                """exception while transforming file ${file.absolutePath}"""
            }
        }
    }
    return {
        result
    }
}
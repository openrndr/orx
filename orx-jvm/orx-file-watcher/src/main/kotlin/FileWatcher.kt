package org.operndr.extras.filewatcher

import com.sun.nio.file.SensitivityWatchEventModifier
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.openrndr.Program
import org.openrndr.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import kotlin.concurrent.thread
private val logger = KotlinLogging.logger {}

class FileWatcher(private val program: Program, val file: File, private val onChange: (File) -> Unit) {
    val path = file.absoluteFile.toPath()
    val parent = path.parent
    val key = pathKeys.getOrPut(parent) {
        parent.register(
                watchService, arrayOf(StandardWatchEventKinds.ENTRY_MODIFY),
                SensitivityWatchEventModifier.HIGH
        )
    }
    val watchers = mutableListOf<() -> Unit>()

    init {
        watchThread
        watching.getOrPut(path) {
            mutableListOf()
        }.add(this)
        keyPaths.getOrPut(key) { parent }
    }

    fun stop() {
        watching[path]?.remove(this)
    }

    internal fun triggerChange() {
        program.launch {
            onChange(file)
            watchers.forEach { it() }
        }
    }
}

private val watchers = mutableMapOf<() -> Any, FileWatcher>()

fun <T> watchFile(program: Program, file: File, transducer: (File) -> T): () -> T {
    var result = transducer(file)
    val watcher = FileWatcher(program, file) {
        try {
            result = transducer(file)
        } catch (e: Throwable) {
            logger.error(e) {
                """exception while transducing file"""
            }
        }
    }

    val function = {
        result
    }

    @Suppress("UNCHECKED_CAST")
    watchers[function as () -> Any] = watcher
    return function
}

/**
 * Stops the watcher
 */
fun <T> (() -> T).stop() {
    @Suppress("UNCHECKED_CAST")
    watchers[this as () -> Any]?.stop()

}

/**
 * Triggers reload
 */
fun <T> (() -> T).triggerChange() {
    @Suppress("UNCHECKED_CAST")
    watchers[this as () -> Any]?.triggerChange()
}


/**
 * add watcher to file watcher
 */
fun <T, R> (() -> T).watch(transducer: (T) -> R): () -> R {

    var result = transducer(this())

    @Suppress("USELESS_CAST")
    watchers[this as () -> Any?]!!.watchers.add {
        result = transducer(this())
    }

    return { result }
}


@JvmName("programWatchFile")
fun <T> Program.watchFile(file: File, transducer: (File) -> T): () -> T = watchFile(this, file, transducer)

private val watching = mutableMapOf<Path, MutableList<FileWatcher>>()
private val pathKeys = mutableMapOf<Path, WatchKey>()
private val keyPaths = mutableMapOf<WatchKey, Path>()
private val waiting = mutableMapOf<Path, Job>()

private val watchService by lazy {
    FileSystems.getDefault().newWatchService()
}

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

fun main() {
    val a = watchFile(Program(), File("README.md")) {
        it.readText()
    }
    a.stop()
    a.triggerChange()
    while (true) {
        println(a())
        Thread.sleep(2000)
    }
}


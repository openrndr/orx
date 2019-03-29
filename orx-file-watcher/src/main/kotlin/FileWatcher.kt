package org.operndr.extras.filewatcher

import com.sun.nio.file.SensitivityWatchEventModifier
import org.openrndr.Program
import org.openrndr.launch
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import kotlin.concurrent.thread

class FileWatcher(private val program: Program, val file: File, private val onChange: (File) -> Unit) {
    init {
        watchThread
        val path = file.absoluteFile.toPath()
        val parent = path.parent
        val key = pathKeys.getOrPut(parent) {
            parent.register(
                    watchService, arrayOf(StandardWatchEventKinds.ENTRY_MODIFY),
                    SensitivityWatchEventModifier.HIGH
            )
        }
        watching.getOrPut(path) {
            mutableListOf()
        }.add(this)
        keyPaths.getOrPut(key) { parent }
    }

    internal fun triggerChange() {
        program.launch {
            onChange(file)
        }
    }
}

fun <T> watchFile(program: Program, file: File, transducer: (File) -> T): () -> T {
    var result = transducer(file)
    FileWatcher(program, file) {
        try {
            result = transducer(file)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
    return {
        result
    }
}

@JvmName("programWatchFile")
fun <T> Program.watchFile(file: File, transducer: (File) -> T): () -> T = watchFile(this, file, transducer)

private val watching = mutableMapOf<Path, MutableList<FileWatcher>>()
private val pathKeys = mutableMapOf<Path, WatchKey>()
private val keyPaths = mutableMapOf<WatchKey, Path>()

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
                watching[fullPath]?.forEach {

                    it.triggerChange()
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



    while (true) {
        println(a())
        Thread.sleep(2000)

    }
}


package org.openrndr.extra.olive

import java.io.File

class Resources(val filterOutExtensions: List<String> = listOf()) {
    private val watchedResources = mutableMapOf<File, Boolean>()

    fun watch(src: File, watchFn: (file: File) -> Unit) {
        src.listFiles()!!.forEach {file ->
            if (file.isFile && !filterOutExtensions.contains(file.extension)) {
                watchedResources[file] = false

                watchFn(file)
            } else if (file.isDirectory) {
                watch(file, watchFn)
            }
        }
    }

    operator fun get(file: File): Boolean? {
        return watchedResources[file]
    }

    operator fun set(file: File, value: Boolean) {
        if (watchedResources.containsKey(file)) {
            watchedResources[file] = value
        }
    }
}
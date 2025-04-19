package org.openrndr.extra.axidraw

import java.io.BufferedInputStream
import java.io.File

/**
 * Determines the appropriate Python executable name based on the operating system.
 *
 * On Windows systems, it returns "python.exe", while on other operating systems, it returns "python3".
 *
 * @return The name of the Python executable appropriate for the current operating system.
 */
fun systemPython(): String {
    val executable = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "python.exe"
    } else {
        "python3"
    }
    return executable
}

/**
 * Returns the path to the Python executable in a given virtual environment.
 * The path varies depending on the operating system.
 *
 * @param venv the directory of the virtual environment
 * @return the absolute path to the Python executable within the virtual environment
 */
fun venvPython(venv: File): String {
    val executable = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "${venv.absolutePath}/Scripts/python.exe"
    } else {
        "${venv.absolutePath}/bin/python"
    }
    return executable
}


fun invokePython(arguments: List<String>, executable: String = systemPython()): String {
    val result: String
    val pb = ProcessBuilder()
        .let {
            it.command(listOf(executable) + arguments)
            //it.redirectError(File("python.error.txt"))
            it.inheritIO()
        }
        .start()
        .let {
            val `is` = it.inputStream
            val bis = BufferedInputStream(`is`)
            val br = bis.bufferedReader()
            result = br.readText().trim()
            val error = it.waitFor()
            if (error != 0) {
                error("Python invoke failed with error $error")
            }
        }

    return result
}
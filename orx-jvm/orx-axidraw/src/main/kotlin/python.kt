package org.openrndr.extra.axidraw

import java.io.File
import java.io.IOException

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


fun invokePython(arguments: List<String>, executable: String = systemPython()): ExecutionResult {
    var result: String
    var errorCode: Int
    try {

        val pb = ProcessBuilder()
            .command(listOf(executable) + arguments)

        val process = pb.start()

        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        Thread {
            process.inputStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    stdoutBuilder.appendLine(line)
                    println("stdout: $line")  // Optional: print to console
                }
            }
        }.start()

        Thread {
            process.errorStream.bufferedReader().use { reader ->
                reader.lineSequence().forEach { line ->
                    stderrBuilder.appendLine(line)
                    println("stderr: $line")  // Optional: print to console
                }
            }
        }.start()

        errorCode = process.waitFor()

        result = buildString {
            append(stdoutBuilder.toString())
            append(stderrBuilder.toString())
        }.trim()
    } catch (e: IOException) {
        error("\n\nPython 3.8 or higher is required but failed to run. Is it installed?\n\n")
    }

    return ExecutionResult(errorCode, result)
}

package org.openrndr.extra.python

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }


/**
 * Class containing the output text and error code resulting from executing command line programs.
 */
class ExecutionResult(val errorCode: Int, val output: String, val error: String)

class PythonInterpreter(val executable: String) {

    fun version(): String? {
        val result = invokePython(listOf("--version"))
        return if (result.errorCode == 0) {
            result.output.trim()
        } else {
            null
        }
    }

    fun invokePython(arguments: List<String>): ExecutionResult {
        val output: String
        val error: String
        var errorCode: Int
        try {

            val pb = ProcessBuilder()
                .command(listOf(executable) + arguments)

            val process = pb.start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            thread(isDaemon = true) {
                process.inputStream.bufferedReader().use { reader ->
                    reader.lineSequence().forEach { line ->
                        stdoutBuilder.appendLine(line)
                        println("stdout: $line")  // Optional: print to console
                    }
                }
            }

            thread(isDaemon = true) {
                process.errorStream.bufferedReader().use { reader ->
                    reader.lineSequence().forEach { line ->
                        stderrBuilder.appendLine(line)
                        println("stderr: $line")  // Optional: print to console
                    }
                }
            }

            errorCode = process.waitFor()

            output = stdoutBuilder.toString()
            error = stderrBuilder.toString()

        } catch (e: IOException) {
            error("\n\nPython 3.8 or higher is required but failed to run. Is it installed?\n\n")
        }

        return ExecutionResult(errorCode, output, error)
    }

}

/**
 * Determines the appropriate Python executable name based on the operating system.
 *
 * On Windows systems, it returns "python.exe", while on other operating systems, it returns "python3".
 *
 * @return The name of the Python executable appropriate for the current operating system.
 */
fun systemPython(): PythonInterpreter {
    val executable = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "python.exe"
    } else {
        "python3"
    }
    return PythonInterpreter(executable)
}

class VirtualEnvironment(val venv: File) {

    val interpreter: PythonInterpreter
        get() {
            val executable = if (System.getProperty("os.name").lowercase().contains("windows")) {
                "${venv.absolutePath}/Scripts/python.exe"
            } else {
                "${venv.absolutePath}/bin/python"
            }
            return PythonInterpreter(executable)
        }

    fun remove() {
        venv.deleteRecursively()
    }
}

fun PythonInterpreter.createVirtualEnv(path: String, reinstall: Boolean = false): VirtualEnvironment {
    if (!File(path).exists() || reinstall) {
        logger.info { "setting up $path Python virtual environment" }
        val result = invokePython(listOf("-m", "venv", path))
        require(result.errorCode == 0) { "Failed to create virtual environment: ${result.errorCode}" }
    }
    return VirtualEnvironment(File(path))
}


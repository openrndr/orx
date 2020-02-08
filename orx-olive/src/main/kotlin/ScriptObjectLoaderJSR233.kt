package org.openrndr.extra.olive

import mu.KotlinLogging
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.MalformedURLException
import java.net.URL
import javax.script.ScriptEngineManager

private val logger = KotlinLogging.logger {}

class LoadException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class ScriptObjectLoader(classLoader: ClassLoader? = Thread.currentThread().contextClassLoader) {
    val engine = run {
        val start = System.currentTimeMillis()
        val engine = ScriptEngineManager(classLoader).getEngineByExtension("kts")
        val end = System.currentTimeMillis()
        logger.info { "creating scripting engine took ${end-start}ms" }
        engine
    }

    init {
        require(engine != null) { "could not create scripting engine" }
    }

    fun <R> safeEval(evaluation: () -> R?) = try {
        evaluation()
    } catch (e: Exception) {
        throw LoadException("Cannot load script", e)
    }

    inline fun <reified T> Any?.castOrError() = takeIf { it is T }?.let { it as T }
            ?: throw IllegalArgumentException("Cannot cast $this to expected type ${T::class}")

    inline fun <reified T> load(script: String): T = safeEval { engine.eval(script) }.castOrError()

    inline fun <reified T> load(reader: Reader): T = safeEval { engine.eval(reader) }.castOrError()

    inline fun <reified T> load(inputStream: InputStream): T = load(inputStream.reader())

    inline fun <reified T> loadAll(vararg inputStream: InputStream): List<T> = inputStream.map(::load)
}


/**
 * Load an object from script.
 */
inline fun <reified T : Any> loadFromScript(fileOrUrl: String, loader: ScriptObjectLoader = ScriptObjectLoader()): T {
    val isUrl = try {
        URL(fileOrUrl); true
    } catch (e: MalformedURLException) {
        false
    }

    val script = if (isUrl) {
        URL(fileOrUrl).readText()
    } else {
        File(fileOrUrl).readText()
    }
    return loader.load(script)
}

/**
 * Load an object from script file
 */
inline fun <reified T : Any> loadFromScript(file: File, loader: ScriptObjectLoader = ScriptObjectLoader()): T =
        loader.load(file.readText())

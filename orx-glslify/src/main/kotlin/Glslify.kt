package org.openrndr.extra.glslify

import mu.KotlinLogging
import org.openrndr.draw.codeFromURL
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.net.ssl.HttpsURLConnection
import kotlin.streams.toList

internal const val BASE_URL = "https://registry.npmjs.org"
internal const val IMPORT_PATT = """#pragma\sglslify:\s([\w]+).*\s=\s*require\('?([\w\-\\/]+).*'?\)$"""
internal const val EXPORT_PATT = """#pragma\sglslify:\s*export\(([\w\-]+)\)"""
internal const val PRAGMA_IDENTIFIER = "#pragma glslify"

internal val shaderExtensions = arrayOf("glsl", "frag")

internal val logger = KotlinLogging.logger {}

private var importTree = mutableSetOf<String>()

fun preprocessGlslifyFromUrl(url: String, glslifyPath: String = "src/main/resources/glslify"): String {
    return preprocessGlslify(codeFromURL(url), glslifyPath)
}


fun preprocessGlslify(source: String, glslifyPath: String = "src/main/resources/glslify"): String {
    importTree = mutableSetOf()

    return source.split("\n").map { line ->
        if (line.trimStart().startsWith(PRAGMA_IDENTIFIER)) {
            Regex(IMPORT_PATT).find(line)?.let {
                if (it.groupValues.size > 1) {
                    val functionName = it.groupValues[1]
                    val module = it.groupValues[2]

                    val (moduleName, shaderPath) = parseModule(module)
                    val importPath = Paths.get(glslifyPath).resolve(moduleName)

                    if (importTree.contains(moduleName)) return@map ""

                    importTree.add(moduleName)

                    return@map glslifyImport(moduleName, importPath, shaderPath, functionName, glslifyPath)
                }
            }
        }

        line
    }.joinToString("\n").trimMargin()
}

private fun glslifyImport(moduleName: String, moduleDir: Path, shaderPath: Path, renameFunctionTo: String? = null, glslifyPath: String): String {
    if (!Files.exists(moduleDir)) {
        fetchModule(moduleName, moduleDir)
    } else {
        logger.trace("[glslify] $moduleName already exists.. Skipping download")
    }

    val shaderFile: Path

    try {
        shaderFile = shaderExtensions.map {
            moduleDir.resolve("$shaderPath.$it")
        }.first { Files.exists(it) }
    } catch (ex: NoSuchElementException) {
        throw error("[glslify] $moduleName: $shaderPath doesn't lead to any shader file")
    }

    var exportName: String? = null

    var shader = Files.lines(shaderFile).map { line ->
        if (line.startsWith(PRAGMA_IDENTIFIER)) {
            Regex(IMPORT_PATT).find(line)?.let {
                val functionName = it.groupValues[1]
                val module = it.groupValues[2]

                val (innerModuleName, innerShaderPath) = parseModule(module)
                val importPath = Paths.get(glslifyPath).resolve(moduleName)

                if (importTree.contains(innerModuleName)) return@map ""

                importTree.add(innerModuleName)

                return@map glslifyImport(innerModuleName, importPath, innerShaderPath, functionName, glslifyPath)
            }

            Regex(EXPORT_PATT).find(line)?.let {
                if (it.groupValues.size > 1) {
                    exportName = it.groupValues[1]
                }

                return@map ""
            }
        }

        line
    }.toList().joinToString("\n")

    if (renameFunctionTo != null && exportName != null) {
        shader = shader.replace( exportName!!, renameFunctionTo)
    }

    return shader.trimMargin()
}

internal fun fetchModule(moduleName: String, moduleDir: Path) {
    val packageUrl = getPackageUrl(moduleName)

    if (packageUrl.isNullOrEmpty()) {
        throw error("[glslify] $moduleName not found")
    }

    try {
        Files.createDirectories(moduleDir)

        val url = URL(packageUrl)
        val con = url.openConnection()
        val tarStream: InputStream = ByteArrayInputStream(con.getInputStream().readBytes())

        extractGzipStream(tarStream, moduleDir.toFile())

        moveFilesUp(moduleDir)

        moduleDir.resolve("package").toFile().deleteRecursively()

        logger.trace("[glslify] $moduleName downloaded")
        (con as HttpsURLConnection).disconnect()
    } catch (ex: Exception) {
        logger.error(ex) { "[glslify]: There was an error getting $moduleName" }
    }
}
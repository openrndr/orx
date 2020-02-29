package org.openrndr.extra.glslify

import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.net.ssl.HttpsURLConnection
import kotlin.streams.toList

internal const val BASE_URL = "https://registry.npmjs.org"
internal const val GLSLIFY_PATH = "src/main/resources/glslify"
internal const val IMPORT_PATT = """#pragma\sglslify:\s*(.*)\s=\s*require\('?([\w\-\\/]+).*'?\)$"""
internal const val EXPORT_PATT = """#pragma\sglslify:\s*export\(([\w\-]+)\)"""

internal val shaderExtensions = arrayOf("glsl", "frag")

private data class GlslifyImport(
        val functionName: String,
        val pkgName: String,
        var exists: Boolean
)

private val logger = KotlinLogging.logger {}

fun glslify(module: String, renameFunctionTo: String? = null): String {
    val (moduleName: String, shaderPath: Path) = parseModule(module)
    val moduleDir = Paths.get("$GLSLIFY_PATH/$moduleName")

    return glslifyImport(moduleName, moduleDir, shaderPath, renameFunctionTo)
}

private fun glslifyImport(moduleName: String, moduleDir: Path, shaderPath: Path, renameFunctionTo: String? = null): String {
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
        logger.trace("[glslify] $moduleName: $shaderPath doesn't lead to any shader file")

        return ""
    }

    val imports = mutableListOf<GlslifyImport>()
    var exportName: String? = null

    var shader = Files.lines(shaderFile).filter { line ->
        line.contains("#pragma").let { isPragma ->
            if (isPragma) {
                Regex(IMPORT_PATT).find(line)?.let {
                    if (it.groupValues.size > 1) {
                        val importPath = Paths.get(GLSLIFY_PATH).resolve(it.groupValues[2])

                        imports.add(GlslifyImport(it.groupValues[1], it.groupValues[2], Files.exists(importPath)))
                    }
                }

                Regex(EXPORT_PATT).find(line)?.let {
                    if (it.groupValues.size > 1) {
                        exportName = it.groupValues[1]
                    }
                }
            }

            !isPragma // we want to exclude any #pragma statements
        }
    }.toList().joinToString("\n")

    val missingImports = imports.filter { !it.exists }

    if (missingImports.isNotEmpty()) {
        missingImports.forEach {
            logger.info("Missing package: ${it.pkgName} - Import name: ${it.functionName}")
        }

        throw error("[glslify] Please declare the missing imports")
    }

    if (renameFunctionTo != null && exportName != null) {
        shader = shader.replace( exportName!!, renameFunctionTo)
    }

    return shader
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

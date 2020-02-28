package org.openrndr.extra.glslify

import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

const val BASE_URL = "https://registry.npmjs.org"
const val GLSLIFY_PATH = "src/main/resources/glslify"
const val IMPORT_PATT = """#pragma\sglslify:\s*(.*)\s=\s*require\('([\w\-]+)'\)"""
const val EXPORT_PATT = """#pragma\sglslify:\s*export\(([\w\-]+)\)"""

internal val shaderExtensions = arrayOf("glsl", "frag")

data class GlslifyImport(
        val functionName: String,
        val pkgName: String,
        var exists: Boolean
)

private val logger = KotlinLogging.logger {}

fun glslify(module: String, renameFunctionTo: String? = null): String {
    val (moduleName: String, shaderPath: String) = parseModule(module)
    val moduleDir = File("$GLSLIFY_PATH/$moduleName")

    val packageUrl = getPackageUrl(moduleName)

    if (packageUrl.isNullOrEmpty()) {
        throw error("[glslify] $moduleName not found")
    }

    if (!moduleDir.exists()) {
        try {
            moduleDir.mkdirs()

            val url = URL(packageUrl)
            val con = url.openConnection()
            val tarStream: InputStream = ByteArrayInputStream(con.getInputStream().readBytes())

            extractGzipStream(tarStream, moduleDir)

            moveFilesUp(moduleDir)

            File("$moduleDir/package").deleteRecursively()

            logger.trace("[glslify] $moduleName downloaded")
            (con as HttpsURLConnection).disconnect()
        } catch (ex: Exception) {
            logger.error(ex) { "[glslify]: There was an error getting $moduleName" }

            return ""
        }
    } else {
        logger.trace("[glslify] $moduleName already exists.. Skipping download")
    }

    val shaderFile: File

    try {
        shaderFile = shaderExtensions.map {
            File("$GLSLIFY_PATH/$moduleName/$shaderPath.$it")
        }.first { it.exists() }
    } catch (ex: NoSuchElementException) {
        logger.trace("[glslify] $moduleName: $shaderPath doesn't lead to any shader file")

        return ""
    }

    val shader = mutableListOf<String>()
    val imports = mutableListOf<GlslifyImport>()
    var exportName: String? = null

    shaderFile.useLines { sequence ->
        for (line in sequence.iterator()) {
            if (line.contains("#pragma")) {
                Regex(IMPORT_PATT).find(line)?.let {
                    if (it.groupValues.size > 1) {
                        val importExists = File("$GLSLIFY_PATH/${it.groupValues[2]}").exists()

                        imports.add(GlslifyImport(it.groupValues[1], it.groupValues[2], importExists))
                    }
                }

                Regex(EXPORT_PATT).find(line)?.let {
                    if (it.groupValues.size > 1) {
                        exportName = it.groupValues[1]
                    }
                }
            } else {
                shader.add(line)
            }
        }
    }

    val missingImports = imports.filter { !it.exists }

    if (missingImports.isNotEmpty()) {
        missingImports.forEach {
            logger.info("Missing package: ${it.pkgName} - Import name: ${it.functionName}")
        }

        throw error("[glslify] Please declare the missing imports")
    }

    var shaderString = shader.joinToString("\n")

    if (renameFunctionTo != null && exportName != null) {
        shaderString = shaderString.replace( exportName!!, renameFunctionTo)
    }

    return shaderString
}

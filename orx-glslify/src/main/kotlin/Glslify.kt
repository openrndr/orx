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
internal const val IMPORT_RELATIVE_PATT = """#pragma\sglslify:\s([\w]+).*\s=\s*require\('?(\./[\w\-\\/]+).*'?\)$"""
internal const val EXPORT_PATT = """#pragma\sglslify:\s*export\(([\w\-]+)\)"""
internal const val PRAGMA_IDENTIFIER = "#pragma glslify"

internal val shaderExtensions = arrayOf("glsl", "frag")

internal val logger = KotlinLogging.logger {}

private var importTree = mutableSetOf<String>()

fun preprocessGlslifyFromUrl(url: String, glslifyPath: String = "src/main/resources/glslify"): String {
    return preprocessGlslify(codeFromURL(url), glslifyPath)
}

data class GlslifyModule(
        val requirePath: String,
        val functionName: String,
        val lineNumber: Int
) {
    private var shaderFile: MutableList<String> = mutableListOf()

    lateinit var moduleName: String
    lateinit var shaderFilePath: Path

    var exportName: String? = null

    private val dependencies = mutableListOf<GlslifyModule>()

    fun import(glslifyPath: String): String {
        val parsed = parseModule(requirePath)

        moduleName = parsed.first
        shaderFilePath = parsed.second

        val importPath = Paths.get(glslifyPath).resolve(moduleName)

        shaderFile = glslifyImport(moduleName, importPath, shaderFilePath)

        dependencies.asReversed().map {
            shaderFile[it.lineNumber] = it.import(glslifyPath)
        }

        var shader = shaderFile.joinToString("\n")

        if (exportName != null) {
            shader = shader.replace(exportName!!, functionName)
        }

        return shader.trimMargin()
    }

    private fun glslifyImport(moduleName: String, moduleDir: Path, shaderPath: Path): MutableList<String> {
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

        return Files.lines(shaderFile).toList().mapIndexed { dependencyLineNumber, line ->
            if (line.startsWith(PRAGMA_IDENTIFIER)) {
                Regex(IMPORT_PATT).find(line)?.let {
                    val functionName = it.groupValues[1]
                    val dependencyRequirePath = it.groupValues[2]

                    if (importTree.contains(dependencyRequirePath)) return@mapIndexed ""

                    importTree.add(dependencyRequirePath)

                    dependencies.add(GlslifyModule(dependencyRequirePath, functionName, dependencyLineNumber))
                }

                Regex(IMPORT_RELATIVE_PATT).find(line)?.let {
                    val functionName = it.groupValues[1]
                    val dependencyRequirePath = it.groupValues[2]

                    if (importTree.contains(dependencyRequirePath)) return@mapIndexed ""

                    importTree.add(dependencyRequirePath)

                    val dependency = moduleDir.fileName.resolve(Paths.get(dependencyRequirePath).normalize()).toString()

                    dependencies.add(GlslifyModule(dependency, functionName, dependencyLineNumber))
                }

                Regex(EXPORT_PATT).find(line)?.let {
                    if (it.groupValues.size > 1) {
                        exportName = it.groupValues[1]
                    }

                    return@mapIndexed ""
                }
            }

            line
        }.toMutableList()
    }
}

internal val stack = mutableListOf<GlslifyModule>()

fun preprocessGlslify(source: String, glslifyPath: String = "src/main/resources/glslify"): String {
    importTree = mutableSetOf()

    stack.clear()

    val mainShader = source.split("\n").mapIndexed { lineNumber, line ->
        if (line.trimStart().startsWith(PRAGMA_IDENTIFIER)) {
            Regex(IMPORT_PATT).find(line)?.let {
                if (it.groupValues.size > 1) {
                    val functionName = it.groupValues[1]
                    val requirePath = it.groupValues[2]

                    if (importTree.contains(requirePath)) return@mapIndexed ""

                    importTree.add(requirePath)

                    stack.add(GlslifyModule(requirePath, functionName, lineNumber))
                }
            }
        }

        line
    }.toMutableList()

    stack.asReversed().forEach {
        mainShader[it.lineNumber] = it.import(glslifyPath)
    }

    return mainShader.joinToString("\n").trimMargin()
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
package org.openrndr.extra.glslify

import org.rauschig.jarchivelib.Archiver
import org.rauschig.jarchivelib.ArchiverFactory
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal fun extractGzipStream(tarStream: InputStream, dest: File) {
    val archiver: Archiver = ArchiverFactory.createArchiver("tar", "gz")

    archiver.extract(tarStream, dest)
}

internal fun moveFilesUp(modulePath: Path) {
    val packageFolder = modulePath.resolve("package")

    val packageFiles = Files.list(packageFolder).filter {
        Files.isDirectory(it) || shaderExtensions.contains(it.fileName.toString().substringAfterLast("."))
    }

    packageFiles.forEach {
        // fileName also retrieves folders ¯\_(ツ)_/¯
        val dest = modulePath.resolve(modulePath.relativize(it).fileName)
        Files.move(it, dest)
    }
}

internal fun parseModule(module: String): Pair<String, Path> {
    val path = Paths.get(module)
    val pathLen = path.nameCount
    val moduleName = path.subpath(0, 1).toString()
    var shaderPath = Paths.get("index")

    if (pathLen > 1) {
        shaderPath = path.subpath(1, pathLen)
    }

    return Pair(moduleName, shaderPath)
}

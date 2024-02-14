package jsyphon

import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.io.File
import java.io.IOException

object JSyphonNative {
    init {
        require(Platform.type == PlatformType.MAC) { "orx-syphon only works on macOS, your platform is not supported" }

        val arch = System.getProperty("os.arch")
        val libraries = when (arch) {
            "x86-64",
            "x86_64",
            "amd64",
            "x64" -> arrayOf("Syphon-x86", "libJSyphon-x86.jnilib")

            else -> arrayOf("Syphon-arm64", "libJSyphon-arm64.jnilib")
        }
        val tempBase = Platform.tempDirectory()
        val tempDir = File(tempBase, "orx-syphon")
        tempDir.mkdirs()
        for (library in libraries) {
            val stream = JSyphonNative::class.java.getResourceAsStream("/jsyphon-natives/$library")
            require(stream != null)
            try {
                val target = File(tempDir, library)
                val bytes = stream.readBytes()
                target.writeBytes(bytes)
            } catch (e: IOException) {
                throw RuntimeException(e)
            } finally {
                stream.close()
            }
        }
        for (library in libraries) {
            System.load(File(tempDir, library).absolutePath)
        }
    }
    fun check() {
        // -- do nothing
    }
}
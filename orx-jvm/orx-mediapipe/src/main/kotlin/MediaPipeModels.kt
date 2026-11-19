package org.openrndr.extra.mediapipe

import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The models of orx-mediapipe and the test images of MediaPipe, from the tag [TAG] of the repository
 * [openrndr/orx-models](https://github.com/openrndr/orx-models): they are downloaded the first time they are used and
 * kept in [cache]. The models are those that `tools/convert_models.py` converts.
 */
object MediaPipeModels {
    /** the tag of the repository whose files are loaded */
    const val TAG = "mediapipe-1"

    /** the address of the files of the tag */
    var baseUrl = "https://raw.githubusercontent.com/openrndr/orx-models/$TAG/mediapipe"

    /**
     * The folder in which the files are kept: the folder of the tag in the folder of the system property
     * `orx.models.cache`, or in `~/.cache/orx-models`
     */
    var cache: File = File(System.getProperty("orx.models.cache") ?: "${System.getProperty("user.home")}/.cache/orx-models", TAG)

    /**
     * The path of the model [name], like `hand_detector.onnx`, downloaded when it is not in [cache] yet, with its labels
     * and its anchors where it has them
     */
    @Synchronized
    fun model(name: String): String {
        val file = File(cache, "onnx/$name")
        if (!file.exists()) {
            download("onnx/$name", file)
            // the labels of classifiers and detectors, the anchors of detectors
            for (companion in listOf(".labels.txt", ".detector.txt")) {
                val companionName = name.removeSuffix(".onnx") + companion
                try {
                    download("onnx/$companionName", File(cache, "onnx/$companionName"))
                } catch (_: FileNotFoundException) {
                    // the model has none
                }
            }
        }
        return file.path
    }

    /** the path of the test image of MediaPipe [name], like `portrait.jpg`, downloaded when it is not in [cache] yet */
    @Synchronized
    fun image(name: String): String {
        val file = File(cache, "images/$name")
        if (!file.exists()) download("images/$name", file)
        return file.path
    }

    /** downloads [path] of the tag to [target], through a file next to it that is moved in place when complete */
    private fun download(path: String, target: File) {
        target.parentFile.mkdirs()
        val partial = File(target.parentFile, target.name + ".part")
        URI("$baseUrl/$path").toURL().openStream().use { input ->
            partial.outputStream().use { input.copyTo(it) }
        }
        Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}

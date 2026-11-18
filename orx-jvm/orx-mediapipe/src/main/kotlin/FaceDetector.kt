package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * A face found by [FaceDetector], in the pixels of the image
 * @param keypoints the right eye, the left eye, the tip of the nose, the center of the mouth, the right ear and the
 * left ear, of the person
 */
class FaceDetection(val bounds: Rectangle, val keypoints: List<Vector2>, val score: Double) {
    val rightEye get() = keypoints[0]
    val leftEye get() = keypoints[1]
    val noseTip get() = keypoints[2]
    val mouth get() = keypoints[3]
    val rightEar get() = keypoints[4]
    val leftEar get() = keypoints[5]
}

/**
 * Finds faces with MediaPipe's BlazeFace short range model, for faces within about two meters of the camera, like
 * MediaPipe's face detector
 * @param modelPath the model converted to ONNX by `tools/convert_models.py`
 * @param minScore the least score of a face
 * @param minSuppressionThreshold the overlap from which faces are taken to be the same face
 */
class FaceDetector(
    modelPath: String = "orx-jvm/orx-mediapipe/models/blaze_face_short_range.onnx",
    var minScore: Double = 0.5,
    var minSuppressionThreshold: Double = 0.3
) : AutoCloseable {
    private val model = OnnxModel(modelPath)
    private val anchors = ssdAnchors(model.size, listOf(8, 16, 16, 16))

    /** the faces in [image] */
    fun detect(image: RgbaImage): List<FaceDetection> {
        val toImage = Affine.letterbox(image.width, image.height, model.size)
        val outputs = model.run(image.tensor(model.size, toImage, -1f, 1f))
        val detections = decodeDetections(
            outputs.getValue("regressors"), outputs.getValue("classificators"), anchors, model.size, 6, minScore
        )
        val size = model.size.toDouble()
        return weightedNonMaxSuppression(detections, minSuppressionThreshold).map { d ->
            val corners = listOf(d.box.corner, d.box.corner + d.box.dimensions).map { toImage(it * size) }
            FaceDetection(bounds(corners), d.keypoints.map { toImage(it * size) }, d.score)
        }
    }

    /** the faces in [image] */
    fun detect(image: ColorBuffer) = detect(image.toRgbaImage())

    override fun close() = model.close()
}

package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A face stylized by [FaceStylizer]
 * @param image the stylized face, the square around the face of [center], [size] and [rotation] in the image
 * @param center the center of the square around the face, in the pixels of the image
 * @param size the length of the sides of the square, in the pixels of the image
 * @param rotation the turn of the square in radians, clockwise in the image
 * @param face the landmarks of the face
 */
class StylizedFace(val image: RgbaImage, val center: Vector2, val size: Double, val rotation: Double, val face: FaceLandmarks)

/**
 * Stylizes faces like MediaPipe's face stylizer: the face found by a [FaceLandmarker] is cut out of the image in a
 * square around its eyes and mouth, which the stylizer model turns into a stylized face of 256 × 256 pixels. The models
 * of MediaPipe are `face_stylizer_color_sketch.onnx`, `face_stylizer_color_ink.onnx` and
 * `face_stylizer_oil_painting.onnx`.
 * @param noise the strength of the noise that the model adds, 1.0 as in MediaPipe; the noise is random, or of [seed]
 */
class FaceStylizer(
    stylizerPath: String = "orx-jvm/orx-mediapipe/models/face_stylizer_color_sketch.onnx",
    detectorPath: String = "orx-jvm/orx-mediapipe/models/face_stylizer_face_detector.onnx",
    landmarksPath: String = "orx-jvm/orx-mediapipe/models/face_stylizer_landmarks_detector.onnx",
    var noise: Double = 1.0,
    seed: Int? = null
) : AutoCloseable {
    private val landmarker = FaceLandmarker(detectorPath, landmarksPath, blendshapesPath = null)
    private val stylizer = OnnxModel(stylizerPath, imageInput = false)
    private val random = seed?.let { Random(it) } ?: Random.Default

    // the image input and the noise inputs of the model
    private val imageInput = stylizer.inputShapes.keys.single { !it.startsWith("noise") }
    private val size = stylizer.inputShapes.getValue(imageInput)[1].toInt()

    /** the stylized face in [image], null without a face */
    fun stylize(image: RgbaImage): StylizedFace? {
        val face = landmarker.detect(image).firstOrNull() ?: return null
        val points = face.landmarks.map { it.xy }
        // like MediaPipe's FaceToRectCalculator: the eyes are the means of their corners, the mouth of its corners
        val rightEye = (points[33] + points[133]) / 2.0
        val leftEye = (points[263] + points[362]) / 2.0
        val mouth = (points[61] + points[291]) / 2.0
        val eyes = (rightEye + leftEye) / 2.0
        val eyeToEye = leftEye - rightEye
        val eyeToMouth = mouth - eyes
        val center = (eyes + eyeToMouth * 0.1).let { Vector2(it.x.roundToInt().toDouble(), it.y.roundToInt().toDouble()) }
        val side = max(eyeToMouth.length * 3.6, eyeToEye.length * 4.0).roundToInt().toDouble()
        val direction = eyeToEye - Vector2(-eyeToMouth.y, eyeToMouth.x)
        val rotation = atan2(direction.y, direction.x)
        val rect = square(center, side, rotation)

        val inputs = mutableMapOf(
            imageInput to (image.tensor(size, size, Affine.rotatedRect(rect, size), -1f, 1f) to longArrayOf(1, size.toLong(), size.toLong(), 3))
        )
        for ((name, shape) in stylizer.inputShapes) {
            if (name == imageInput) continue
            val concrete = shape.map { if (it < 0) 1L else it }.toLongArray()
            val count = concrete.fold(1L) { a, b -> a * b }.toInt()
            val buffer = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            repeat(count) { buffer.put((gaussian() * noise).toFloat()) }
            buffer.rewind()
            inputs[name] = buffer to concrete
        }
        val output = stylizer.run(inputs).values.single()
        // colors from 0 to 1
        val pixels = ByteArray(size * size * 4)
        for (i in 0 until size * size) {
            for (k in 0..2) pixels[i * 4 + k] = (output[i * 3 + k].coerceIn(0f, 1f) * 255f).roundToInt().toByte()
            pixels[i * 4 + 3] = 0xff.toByte()
        }
        return StylizedFace(RgbaImage(size, size, pixels), center, side, rotation, face)
    }

    /** the stylized face in [image], null without a face */
    fun stylize(image: ColorBuffer) = stylize(image.toRgbaImage())

    /** a normally distributed number, by the method of Box and Muller */
    private fun gaussian(): Double {
        val u = 1.0 - random.nextDouble()
        val v = random.nextDouble()
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u)) * kotlin.math.cos(2.0 * Math.PI * v)
    }

    override fun close() {
        landmarker.close()
        stylizer.close()
    }
}

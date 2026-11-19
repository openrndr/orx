package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp
import kotlin.math.max

/**
 * A face found by [FaceLandmarker]
 * @param landmarks the 478 landmarks of the face, the 468 of the mesh and 5 of each iris, x and y in the pixels of the
 * image, z the depth relative to the center of the head, in about the same scale as x, smaller is closer to the camera
 * @param blendshapes the 52 blendshapes of the face, how much it shows each expression, from 0 to 1, by the names in
 * [BLENDSHAPE_NAMES]; empty without a blendshapes model
 * @param score how sure the model is that this is a face
 */
class FaceLandmarks(
    val landmarks: List<Vector3>,
    val blendshapes: Map<String, Double>,
    val score: Double
) {
    companion object {
        /** the triangles of the mesh of the 468 landmarks, three indices of landmarks each */
        val TRIANGLES: List<IntArray> by lazy {
            val text = FaceLandmarks::class.java.getResourceAsStream("face-mesh-triangles.txt")!!.bufferedReader().readText()
            text.lines().filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
                line.trim().split(" ").map { it.toInt() }.toIntArray()
            }
        }

        // the connections of the contours of the face, like MediaPipe's face landmarks connections; left and right are
        // those of the person
        val LIPS = listOf(
            61 to 146, 146 to 91, 91 to 181, 181 to 84, 84 to 17, 17 to 314, 314 to 405, 405 to 321,
            321 to 375, 375 to 291, 61 to 185, 185 to 40, 40 to 39, 39 to 37, 37 to 0, 0 to 267,
            267 to 269, 269 to 270, 270 to 409, 409 to 291, 78 to 95, 95 to 88, 88 to 178, 178 to 87,
            87 to 14, 14 to 317, 317 to 402, 402 to 318, 318 to 324, 324 to 308, 78 to 191, 191 to 80,
            80 to 81, 81 to 82, 82 to 13, 13 to 312, 312 to 311, 311 to 310, 310 to 415, 415 to 308
        )
        val LEFT_EYE = listOf(
            263 to 249, 249 to 390, 390 to 373, 373 to 374, 374 to 380, 380 to 381, 381 to 382, 382 to 362,
            263 to 466, 466 to 388, 388 to 387, 387 to 386, 386 to 385, 385 to 384, 384 to 398, 398 to 362
        )
        val LEFT_EYEBROW = listOf(
            276 to 283, 283 to 282, 282 to 295, 295 to 285, 300 to 293, 293 to 334, 334 to 296, 296 to 336
        )
        val LEFT_IRIS = listOf(
            474 to 475, 475 to 476, 476 to 477, 477 to 474
        )
        val RIGHT_EYE = listOf(
            33 to 7, 7 to 163, 163 to 144, 144 to 145, 145 to 153, 153 to 154, 154 to 155, 155 to 133,
            33 to 246, 246 to 161, 161 to 160, 160 to 159, 159 to 158, 158 to 157, 157 to 173, 173 to 133
        )
        val RIGHT_EYEBROW = listOf(
            46 to 53, 53 to 52, 52 to 65, 65 to 55, 70 to 63, 63 to 105, 105 to 66, 66 to 107
        )
        val RIGHT_IRIS = listOf(
            469 to 470, 470 to 471, 471 to 472, 472 to 469
        )
        val FACE_OVAL = listOf(
            10 to 338, 338 to 297, 297 to 332, 332 to 284, 284 to 251, 251 to 389, 389 to 356, 356 to 454,
            454 to 323, 323 to 361, 361 to 288, 288 to 397, 397 to 365, 365 to 379, 379 to 378, 378 to 400,
            400 to 377, 377 to 152, 152 to 148, 148 to 176, 176 to 149, 149 to 150, 150 to 136, 136 to 172,
            172 to 58, 58 to 132, 132 to 93, 93 to 234, 234 to 127, 127 to 162, 162 to 21, 21 to 54,
            54 to 103, 103 to 67, 67 to 109, 109 to 10
        )

        /** the contours of the face: the lips, the eyes, the eyebrows, the irises and the oval of the face */
        val CONTOURS = LIPS + LEFT_EYE + LEFT_EYEBROW + LEFT_IRIS + RIGHT_EYE + RIGHT_EYEBROW + RIGHT_IRIS + FACE_OVAL

        /** the names of the blendshapes, like those of ARKit */
        val BLENDSHAPE_NAMES = listOf(
            "_neutral", "browDownLeft", "browDownRight", "browInnerUp", "browOuterUpLeft", "browOuterUpRight",
            "cheekPuff", "cheekSquintLeft", "cheekSquintRight", "eyeBlinkLeft", "eyeBlinkRight", "eyeLookDownLeft",
            "eyeLookDownRight", "eyeLookInLeft", "eyeLookInRight", "eyeLookOutLeft", "eyeLookOutRight", "eyeLookUpLeft",
            "eyeLookUpRight", "eyeSquintLeft", "eyeSquintRight", "eyeWideLeft", "eyeWideRight", "jawForward",
            "jawLeft", "jawOpen", "jawRight", "mouthClose", "mouthDimpleLeft", "mouthDimpleRight",
            "mouthFrownLeft", "mouthFrownRight", "mouthFunnel", "mouthLeft", "mouthLowerDownLeft", "mouthLowerDownRight",
            "mouthPressLeft", "mouthPressRight", "mouthPucker", "mouthRight", "mouthRollLower", "mouthRollUpper",
            "mouthShrugLower", "mouthShrugUpper", "mouthSmileLeft", "mouthSmileRight", "mouthStretchLeft", "mouthStretchRight",
            "mouthUpperUpLeft", "mouthUpperUpRight", "noseSneerLeft", "noseSneerRight"
        )

        /** the landmarks that the blendshapes model reads */
        internal val BLENDSHAPE_LANDMARKS = intArrayOf(
            0, 1, 4, 5, 6, 7, 8, 10, 13, 14, 17, 21, 33, 37, 39, 40, 46, 52, 53, 54,
            55, 58, 61, 63, 65, 66, 67, 70, 78, 80, 81, 82, 84, 87, 88, 91, 93, 95, 103, 105,
            107, 109, 127, 132, 133, 136, 144, 145, 146, 148, 149, 150, 152, 153, 154, 155, 157, 158, 159, 160,
            161, 162, 163, 168, 172, 173, 176, 178, 181, 185, 191, 195, 197, 234, 246, 249, 251, 263, 267, 269,
            270, 276, 282, 283, 284, 285, 288, 291, 293, 295, 296, 297, 300, 308, 310, 311, 312, 314, 317, 318,
            321, 323, 324, 332, 334, 336, 338, 356, 361, 362, 365, 373, 374, 375, 377, 378, 379, 380, 381, 382,
            384, 385, 386, 387, 388, 389, 390, 397, 398, 400, 402, 405, 409, 415, 454, 466, 468, 469, 470, 471,
            472, 473, 474, 475, 476, 477
        )
    }
}

/**
 * Finds faces and their landmarks, the face mesh, like MediaPipe's face landmarker: the faces are found in the whole
 * image by [FaceDetector], the landmarks in a rectangle around each face, turned such that its eyes are level. In
 * video, with [tracking], the faces are followed from frame to frame, they are looked for only while fewer than
 * [maxFaces] are followed.
 * @param blendshapesPath the blendshapes model, or null to find no blendshapes
 * @param maxFaces the most faces that are found
 * @param minDetectionScore the least score of a face in the whole image
 * @param minScore the least score of a face in the rectangle around it
 * @param tracking true to follow the faces in the frames of a video
 */
class FaceLandmarker(
    detectorPath: String = MediaPipeModels.model("blaze_face_short_range.onnx"),
    landmarksPath: String = MediaPipeModels.model("face_landmarks_detector.onnx"),
    blendshapesPath: String? = MediaPipeModels.model("face_blendshapes.onnx"),
    var maxFaces: Int = 1,
    minDetectionScore: Double = 0.5,
    var minScore: Double = 0.5,
    tracking: Boolean = false
) : AutoCloseable {
    /** true to follow the faces in the frames of a video, false to find them in every image anew */
    var tracking: Boolean = tracking
        set(value) {
            field = value
            followed = emptyList()
        }

    private val detector = FaceDetector(detectorPath, minDetectionScore)
    private val landmarker = OnnxModel(landmarksPath)
    private val blendshapes = blendshapesPath?.let { OnnxModel(it, imageInput = false) }

    /** the rectangles around the faces found in the frame before, for [tracking] */
    private var followed: List<RotatedRect> = emptyList()

    /** the faces in [image], with [tracking] in the frame after that of the previous call */
    fun detect(image: RgbaImage): List<FaceLandmarks> {
        val rects = if (tracking) followed else emptyList()
        val found = if (rects.size < maxFaces) {
            detector.detect(image).take(maxFaces).map { faceRect(it) }.filter { face -> rects.none { iou(it.bounds, face.bounds) > 0.5 } }
        } else emptyList()
        val faces = mutableListOf<FaceLandmarks>()
        val faceRects = mutableListOf<RotatedRect>()
        for (rect in (rects + found).take(maxFaces)) {
            val face = landmarks(image, rect) ?: continue
            val next = landmarksRect(face.landmarks.map { it.xy })
            // two rectangles that came to follow the same face
            if (faceRects.any { iou(it.bounds, next.bounds) > 0.5 }) continue
            faces.add(face)
            faceRects.add(next)
        }
        followed = if (tracking) faceRects else emptyList()
        return faces
    }

    /** the faces in [image] */
    fun detect(image: ColorBuffer) = detect(image.toRgbaImage())

    /** the rectangle around a face found by the detector: half as large again as its box, its eyes level */
    private fun faceRect(face: FaceDetection) =
        RotatedRect(face.bounds.center, face.bounds.width * 1.5, face.bounds.height * 1.5, rotationTo(face.rightEye, face.leftEye, 0.0))

    /**
     * The rectangle around a face in the next frame, from its landmarks: the square around the bounds of the landmarks,
     * half as large again as its longest side, turned such that the outer corners of the eyes are level
     */
    private fun landmarksRect(landmarks: List<Vector2>): RotatedRect {
        val box = bounds(landmarks)
        return square(box.center, max(box.width, box.height) * 1.5, rotationTo(landmarks[33], landmarks[263], 0.0))
    }

    /** the landmarks of the face in [rect] of [image], null when the model is not sure of a face there */
    internal fun landmarks(image: RgbaImage, rect: RotatedRect): FaceLandmarks? {
        val size = landmarker.size
        val toImage = Affine.rotatedRect(rect, size)
        val outputs = landmarker.run(image.tensor(size, toImage, 0f, 1f))
        // by their sizes, the outputs of the models of the face landmarker and of the face stylizer are named apart
        val raw = outputs.values.single { it.size == 478 * 3 }
        val score = sigmoid((outputs["Identity_1"] ?: outputs.values.first { it.size == 1 })[0])
        if (score < minScore) return null
        val scale = rect.width / size
        val landmarks = (0 until raw.size / 3).map { i ->
            val p = toImage(Vector2(raw[3 * i].toDouble(), raw[3 * i + 1].toDouble()))
            Vector3(p.x, p.y, raw[3 * i + 2] * scale)
        }
        return FaceLandmarks(landmarks, blendshapes(landmarks), score)
    }

    /** the blendshapes of a face from the positions of its landmarks in the image */
    private fun blendshapes(landmarks: List<Vector3>): Map<String, Double> {
        val model = blendshapes ?: return emptyMap()
        val points = FaceLandmarks.BLENDSHAPE_LANDMARKS
        val input = ByteBuffer.allocateDirect(points.size * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in points) {
            input.put(landmarks[i].x.toFloat())
            input.put(landmarks[i].y.toFloat())
        }
        input.rewind()
        val scores = model.run(input, longArrayOf(1, points.size.toLong(), 2)).values.single()
        return FaceLandmarks.BLENDSHAPE_NAMES.zip(scores.map { it.toDouble() }).toMap()
    }

    override fun close() {
        detector.close()
        landmarker.close()
        blendshapes?.close()
    }
}

private fun sigmoid(x: Float) = 1.0 / (1.0 + exp(-x.toDouble()))

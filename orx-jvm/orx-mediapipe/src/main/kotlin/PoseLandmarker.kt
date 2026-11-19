package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * A body found by [PoseLandmarker]
 * @param landmarks the 33 landmarks of the body, x and y in the pixels of the image, z the depth relative to the center
 * of the hips, in about the same scale as x, smaller is closer to the camera
 * @param visibility for each landmark how likely it is to be visible, not hidden behind the body or something else
 * @param presence for each landmark how likely it is to be in the image
 * @param worldLandmarks the landmarks in meters, around the center of the hips
 * @param score how sure the model is that this is a body
 * @param mask how likely the pixels around the body are to be of the body
 */
class PoseLandmarks(
    val landmarks: List<Vector3>,
    val visibility: List<Double>,
    val presence: List<Double>,
    val worldLandmarks: List<Vector3>,
    val score: Double,
    val mask: PoseMask
) {
    companion object {
        /** the pairs of the landmarks that are connected, like MediaPipe's pose connections */
        val CONNECTIONS = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 7, 0 to 4, 4 to 5, 5 to 6, 6 to 8, 9 to 10,
            11 to 12, 11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21, 17 to 19,
            12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22, 18 to 20,
            11 to 23, 12 to 24, 23 to 24, 23 to 25, 24 to 26, 25 to 27, 26 to 28,
            27 to 29, 28 to 30, 29 to 31, 30 to 32, 27 to 31, 28 to 32
        )
    }
}

/**
 * Finds bodies and their landmarks like MediaPipe's pose landmarker, BlazePose: the bodies are found in the whole
 * image, the landmarks in a square around each body, turned such that it stands upright. In video, with [tracking],
 * the bodies are followed from frame to frame, they are looked for only while fewer than [maxPoses] are followed.
 * @param landmarksPath the landmark model, `pose_landmarks_detector_full.onnx`, the faster and less precise
 * `pose_landmarks_detector_lite.onnx` or the slower and more precise `pose_landmarks_detector_heavy.onnx`
 * @param maxPoses the most bodies that are found
 * @param minDetectionScore the least score of a body in the whole image
 * @param minScore the least score of a body in the square around it
 * @param tracking true to follow the bodies in the frames of a video
 */
class PoseLandmarker(
    detectorPath: String = MediaPipeModels.model("pose_detector.onnx"),
    landmarksPath: String = MediaPipeModels.model("pose_landmarks_detector_full.onnx"),
    var maxPoses: Int = 1,
    var minDetectionScore: Double = 0.5,
    var minScore: Double = 0.5,
    tracking: Boolean = false
) : AutoCloseable {
    /** true to follow the bodies in the frames of a video, false to find them in every image anew */
    var tracking: Boolean = tracking
        set(value) {
            field = value
            followed = emptyList()
        }

    private val detector = MediaPipeModel(detectorPath)
    private val landmarker = MediaPipeModel(landmarksPath)
    private val anchors = ssdAnchors(detector.size, listOf(8, 16, 32, 32, 32))

    /** the squares around the bodies found in the frame before, for [tracking] */
    private var followed: List<RotatedRect> = emptyList()

    /** the bodies in [image], with [tracking] in the frame after that of the previous call */
    fun detect(image: RgbaImage): List<PoseLandmarks> {
        val squares = if (tracking) followed else emptyList()
        val found = if (squares.size < maxPoses) {
            bodySquares(image).filter { body -> squares.none { iou(it.bounds, body.bounds) > 0.5 } }
        } else emptyList()
        val poses = mutableListOf<PoseLandmarks>()
        val poseSquares = mutableListOf<RotatedRect>()
        for (square in (squares + found).take(maxPoses)) {
            val (pose, next) = landmarks(image, square) ?: continue
            // two squares that came to follow the same body
            if (poseSquares.any { iou(it.bounds, next.bounds) > 0.5 }) continue
            poses.add(pose)
            poseSquares.add(next)
        }
        followed = if (tracking) poseSquares else emptyList()
        return poses
    }

    /** the bodies in [image] */
    fun detect(image: ColorBuffer) = detect(image.toRgbaImage())

    /** the squares around the bodies in [image] */
    private fun bodySquares(image: RgbaImage): List<RotatedRect> {
        val toImage = Affine.letterbox(image.width, image.height, detector.size)
        val outputs = detector.run(image.tensor(detector.size, toImage, -1f, 1f))
        val bodies = weightedNonMaxSuppression(
            decodeDetections(outputs.getValue("Identity"), outputs.getValue("Identity_1"), anchors, detector.size, 4, minDetectionScore),
            0.3
        ).take(maxPoses)
        val size = detector.size.toDouble()
        return bodies.map { body -> alignedSquare(toImage(body.keypoints[0] * size), toImage(body.keypoints[1] * size)) }
    }

    /**
     * The square around a body, from the center of its hips, [center], and a point above it whose distance gives the
     * size of the body, [scale], like MediaPipe's AlignmentPointsRectsCalculator: turned such that [scale] is above
     * [center], a quarter larger than twice their distance
     */
    private fun alignedSquare(center: Vector2, scale: Vector2) =
        square(center, center.distanceTo(scale) * 2.0 * 1.25, uprightRotation(center, scale))

    /** the landmarks of the body in [square], and the square around it in the next frame */
    private fun landmarks(image: RgbaImage, square: RotatedRect): Pair<PoseLandmarks, RotatedRect>? {
        val size = landmarker.size
        val toImage = Affine.rotatedRect(square, size)
        val outputs = landmarker.run(image.tensor(size, toImage, 0f, 1f))
        val score = outputs.getValue("Identity_1")[0].toDouble()
        if (score < minScore) return null

        // 39 landmarks, x, y and z in the pixels of the input, visibility and presence before the sigmoid
        val raw = outputs.getValue("Identity")
        val count = raw.size / 5
        val positions = (0 until count).map { i -> Vector2(raw[5 * i].toDouble(), raw[5 * i + 1].toDouble()) / size.toDouble() }
        val refined = refineFromHeatmap(positions, outputs.getValue("Identity_3"))
        val scale = square.width / size
        val landmarks = (0 until count).map { i ->
            val p = toImage(refined[i] * size.toDouble())
            // the depth is not normalized, unlike that of hands
            Vector3(p.x, p.y, raw[5 * i + 2] * scale)
        }
        val world = outputs.getValue("Identity_4")
        val c = cos(square.rotation)
        val s = sin(square.rotation)
        val worldLandmarks = (0 until 33).map { i ->
            val x = world[3 * i].toDouble()
            val y = world[3 * i + 1].toDouble()
            Vector3(c * x - s * y, s * x + c * y, world[3 * i + 2].toDouble())
        }
        val mask = outputs.getValue("Identity_2").let { logits -> FloatArray(logits.size) { sigmoid(logits[it]).toFloat() } }
        val pose = PoseLandmarks(
            landmarks.take(33),
            (0 until 33).map { sigmoid(raw[5 * it + 3]) },
            (0 until 33).map { sigmoid(raw[5 * it + 4]) },
            worldLandmarks,
            score,
            PoseMask(size, size, mask, square.center, square.width, square.rotation)
        )
        // the two landmarks after the 33 of the body are the center of the hips and the point that gives its size
        return pose to alignedSquare(landmarks[33].xy, landmarks[34].xy)
    }

    /**
     * The landmarks at the center of mass of the heatmap around them, in a window of 7 × 7, where the heatmap is
     * at least 0.5, like MediaPipe's RefineLandmarksFromHeatmapCalculator; positions from 0 to 1 in the input
     */
    private fun refineFromHeatmap(positions: List<Vector2>, heatmap: FloatArray): List<Vector2> {
        val channels = positions.size
        val side = kotlin.math.sqrt((heatmap.size / channels).toDouble()).toInt()
        val offset = 3
        return positions.mapIndexed { index, p ->
            val col = (p.x * side).toInt()
            val row = (p.y * side).toInt()
            if (col < 0 || col >= side || row < 0 || row >= side) return@mapIndexed p
            var sum = 0.0
            var weightedCol = 0.0
            var weightedRow = 0.0
            var maxConfidence = 0.0
            for (r in max(0, row - offset) until min(side, row + offset + 1)) {
                for (c in max(0, col - offset) until min(side, col + offset + 1)) {
                    val confidence = sigmoid(heatmap[(r * side + c) * channels + index])
                    sum += confidence
                    maxConfidence = max(maxConfidence, confidence)
                    weightedCol += c * confidence
                    weightedRow += r * confidence
                }
            }
            if (maxConfidence >= 0.5 && sum > 0.0) Vector2(weightedCol / side / sum, weightedRow / side / sum) else p
        }
    }

    override fun close() {
        detector.close()
        landmarker.close()
    }
}

private fun sigmoid(x: Float) = 1.0 / (1.0 + exp(-x.toDouble()))

/**
 * The mask of a body found by [PoseLandmarker]: how likely the pixels of the square around the body, of [center],
 * [size] and [rotation] in the image, are to be of the body, in [width] × [height] values, row by row from the top
 * @param rotation the turn of the square in radians, clockwise in the image
 */
class PoseMask(
    val width: Int,
    val height: Int,
    val values: FloatArray,
    val center: Vector2,
    val size: Double,
    val rotation: Double
) {
    private val fromImage = Affine.rotatedRect(square(center, size, rotation), width).inversed()

    /** how likely the pixel at ([x], [y]) of the image is to be of the body, 0 outside the square, like MediaPipe's mask */
    fun at(x: Double, y: Double): Double {
        val p = fromImage(Vector2(x, y))
        val x0 = kotlin.math.floor(p.x).toInt()
        val y0 = kotlin.math.floor(p.y).toInt()
        val fx = p.x - x0
        val fy = p.y - y0
        fun v(i: Int, j: Int) = if (i < 0 || j < 0 || i >= width || j >= height) 0.0 else values[j * width + i].toDouble()
        return (v(x0, y0) * (1 - fx) + v(x0 + 1, y0) * fx) * (1 - fy) + (v(x0, y0 + 1) * (1 - fx) + v(x0 + 1, y0 + 1) * fx) * fy
    }

    /**
     * Writes the mask into the red channel of [target], a color buffer of [width] × [height] pixels in floating point,
     * which is drawn in the square around the body
     */
    fun write(target: ColorBuffer) = writeMask(values, width, height, target)
}

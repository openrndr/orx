package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.Rectangle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * A hand found by [HandLandmarker]
 * @param landmarks the 21 landmarks of the hand, x and y in the pixels of the image, z the depth relative to the wrist,
 * in about the same scale as x, smaller is closer to the camera
 * @param worldLandmarks the landmarks in meters, around the center of the hand
 * @param rightHanded true for a right hand, as a camera that is not mirrored sees it
 * @param handednessScore how sure the model is of [rightHanded], from 0.5 to 1
 * @param score how sure the model is that this is a hand
 */
class HandLandmarks(
    val landmarks: List<Vector3>,
    val worldLandmarks: List<Vector3>,
    val rightHanded: Boolean,
    val handednessScore: Double,
    val score: Double
) {
    companion object {
        /** the pairs of the landmarks that are connected by bones */
        val CONNECTIONS = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4,
            0 to 5, 5 to 6, 6 to 7, 7 to 8,
            5 to 9, 9 to 10, 10 to 11, 11 to 12,
            9 to 13, 13 to 14, 14 to 15, 15 to 16,
            13 to 17, 0 to 17, 17 to 18, 18 to 19, 19 to 20
        )
    }
}

/**
 * Finds hands and their landmarks like MediaPipe's hand landmarker: the palms are found in the whole image, the
 * landmarks in a square around each palm, turned such that the fingers point up. In video, with [tracking], the hands
 * are followed from frame to frame, the palms are looked for only while fewer than [maxHands] hands are followed.
 * @param maxHands the most hands that are found
 * @param minDetectionScore the least score of a palm
 * @param minScore the least score of a hand in the square around a palm, or around where it was in the frame before
 * @param tracking true to follow the hands in the frames of a video
 */
class HandLandmarker(
    handDetectorPath: String = "orx-jvm/orx-mediapipe/models/hand_detector.onnx",
    landmarksPath: String = "orx-jvm/orx-mediapipe/models/hand_landmarks_detector.onnx",
    var maxHands: Int = 2,
    var minDetectionScore: Double = 0.5,
    var minScore: Double = 0.5,
    tracking: Boolean = false
) : AutoCloseable {
    /** true to follow the hands in the frames of a video, false to find them in every image anew */
    var tracking: Boolean = tracking
        set(value) {
            field = value
            followed = emptyList()
        }

    /** the squares around the hands found in the frame before, for [tracking] */
    private var followed: List<Square> = emptyList()

    private val detector = OnnxModel(handDetectorPath)
    private val landmarker = OnnxModel(landmarksPath)
    private val anchors = ssdAnchors(detector.size, listOf(8, 16, 16, 16))

    /** the hands in [image], with [tracking] in the frame after that of the previous call */
    fun detect(image: RgbaImage): List<HandLandmarks> {
        val squares = if (tracking) followed else emptyList()
        // palms where no hand is followed
        val found = if (squares.size < maxHands) {
            palmSquares(image).filter { palm -> squares.none { iou(it.bounds, palm.bounds) > 0.5 } }
        } else emptyList()
        val hands = mutableListOf<HandLandmarks>()
        val handSquares = mutableListOf<Square>()
        for (square in (squares + found).take(maxHands)) {
            val hand = landmarks(image, square) ?: continue
            // two squares that came to follow the same hand
            val next = landmarksSquare(hand.landmarks.map { it.xy })
            if (handSquares.any { iou(it.bounds, next.bounds) > 0.5 }) continue
            hands.add(hand)
            handSquares.add(next)
        }
        followed = if (tracking) handSquares else emptyList()
        return hands
    }

    /** the squares around the hands of the palms in [image] */
    private fun palmSquares(image: RgbaImage): List<Square> {
        val toImage = Affine.letterbox(image.width, image.height, detector.size)
        val outputs = detector.run(image.tensor(detector.size, toImage, 0f, 1f))
        val palms = weightedNonMaxSuppression(
            decodeDetections(outputs.getValue("Identity"), outputs.getValue("Identity_1"), anchors, detector.size, 7, minDetectionScore),
            0.3
        ).take(maxHands)
        val size = detector.size.toDouble()
        return palms.map { palm ->
            val corners = listOf(palm.box.corner, palm.box.corner + palm.box.dimensions).map { toImage(it * size) }
            handSquare(bounds(corners), palm.keypoints.map { toImage(it * size) })
        }
    }

    /** the hands in [image] */
    fun detect(image: ColorBuffer) = detect(image.toRgbaImage())

    /** a square around a hand, turned by [rotation] radians */
    private class Square(val center: Vector2, val side: Double, val rotation: Double) {
        /** the bounds of the turned square */
        val bounds: Rectangle
            get() {
                val half = side / 2.0 * (abs(cos(rotation)) + abs(sin(rotation)))
                return Rectangle(center.x - half, center.y - half, 2.0 * half, 2.0 * half)
            }
    }

    /**
     * The square around a hand in the next frame, from its landmarks, like MediaPipe's HandLandmarksToRectCalculator: turned
     * such that the knuckles of the index, middle and ring fingers are above the wrist, around the palm and the bases of
     * the fingers
     */
    private fun landmarksSquare(landmarks: List<Vector2>): Square {
        val points = listOf(0, 1, 2, 3, 5, 6, 9, 10, 13, 14, 17, 18).map { landmarks[it] }
        val wrist = landmarks[0]
        val knuckles = ((landmarks[5] + landmarks[13]) / 2.0 + landmarks[9]) / 2.0
        val rotation = normalizedRadians(PI / 2.0 - atan2(-(knuckles.y - wrist.y), knuckles.x - wrist.x))
        // the bounds of the points turned upright, around the center of their bounds
        val center = bounds(points).center
        val c = cos(-rotation)
        val s = sin(-rotation)
        val upright = bounds(points.map { p -> (p - center).let { Vector2(c * it.x - s * it.y, s * it.x + c * it.y) } })
        val uc = upright.center
        val boxCenter = center + Vector2(cos(rotation) * uc.x - sin(rotation) * uc.y, sin(rotation) * uc.x + cos(rotation) * uc.y)
        // a tenth of the height towards the fingers, a square of twice the longest side
        val shiftY = -0.1
        val shifted = boxCenter + Vector2(-upright.height * shiftY * sin(rotation), upright.height * shiftY * cos(rotation))
        return Square(shifted, max(upright.width, upright.height) * 2.0, rotation)
    }

    /** the square around the hand of a palm, from the wrist to the middle finger upwards */
    private fun handSquare(box: Rectangle, keypoints: List<Vector2>): Square {
        val wrist = keypoints[0]
        val middle = keypoints[2]
        // the turn that makes the middle finger point up
        val rotation = normalizedRadians(PI / 2.0 - atan2(-(middle.y - wrist.y), middle.x - wrist.x))
        // half the height of the palm towards the fingers, a square of 2.6 times its longest side
        val shiftY = -0.5
        val center = box.center + Vector2(-box.height * shiftY * sin(rotation), box.height * shiftY * cos(rotation))
        return Square(center, max(box.width, box.height) * 2.6, rotation)
    }

    private fun landmarks(image: RgbaImage, square: Square): HandLandmarks? {
        val size = landmarker.size
        val toImage = Affine.rotatedSquare(square.center, square.side, square.rotation, size)
        val outputs = landmarker.run(image.tensor(size, toImage, 0f, 1f))
        val score = outputs.getValue("Identity_1")[0].toDouble()
        if (score < minScore) return null
        val raw = outputs.getValue("Identity")
        val scale = square.side / size
        val landmarks = (0 until 21).map { i ->
            val p = toImage(Vector2(raw[3 * i].toDouble(), raw[3 * i + 1].toDouble()))
            // the depth is normalized like MediaPipe's, by 0.4
            Vector3(p.x, p.y, raw[3 * i + 2] * scale / 0.4)
        }
        // the world landmarks turned like the square
        val world = outputs.getValue("Identity_3")
        val c = cos(square.rotation)
        val s = sin(square.rotation)
        val worldLandmarks = (0 until 21).map { i ->
            val x = world[3 * i].toDouble()
            val y = world[3 * i + 1].toDouble()
            Vector3(c * x - s * y, s * x + c * y, world[3 * i + 2].toDouble())
        }
        val right = outputs.getValue("Identity_2")[0].toDouble()
        return HandLandmarks(landmarks, worldLandmarks, right > 0.5, max(right, 1.0 - right), score)
    }

    override fun close() {
        detector.close()
        landmarker.close()
    }
}

/** [angle] in -π to π */
private fun normalizedRadians(angle: Double) = angle - 2.0 * PI * floor((angle + PI) / (2.0 * PI))

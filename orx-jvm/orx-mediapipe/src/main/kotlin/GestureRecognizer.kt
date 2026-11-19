package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max

/**
 * A hand and its gesture, found by [GestureRecognizer]
 * @param gesture the most likely gesture, one of [GestureRecognizer.GESTURES], `None` for none of them
 * @param score how likely [gesture] is
 * @param scores how likely each gesture is
 */
class HandGesture(val hand: HandLandmarks, val gesture: String, val score: Double, val scores: Map<String, Double>)

/**
 * Recognizes the gestures of hands like MediaPipe's gesture recognizer: the landmarks of the hands found by a
 * [HandLandmarker], relative to the wrist, are classified into the gestures of [GESTURES]. The hands can be followed in
 * video like those of the [HandLandmarker], with [tracking].
 */
class GestureRecognizer(
    handDetectorPath: String = MediaPipeModels.model("hand_detector.onnx"),
    landmarksPath: String = MediaPipeModels.model("hand_landmarks_detector.onnx"),
    embedderPath: String = MediaPipeModels.model("gesture_embedder.onnx"),
    classifierPath: String = MediaPipeModels.model("canned_gesture_classifier.onnx"),
    maxHands: Int = 2,
    tracking: Boolean = false
) : AutoCloseable {
    companion object {
        /** the gestures, in the order of the classifier */
        val GESTURES = listOf("None", "Closed_Fist", "Open_Palm", "Pointing_Up", "Thumb_Down", "Thumb_Up", "Victory", "ILoveYou")
    }

    /** the landmarker of the hands */
    val landmarker = HandLandmarker(handDetectorPath, landmarksPath, maxHands = maxHands, tracking = tracking)
    private val embedder = MediaPipeModel(embedderPath, imageInput = false)
    private val classifier = MediaPipeModel(classifierPath, imageInput = false)

    /** the hands in [image] and their gestures, with tracking in the frame after that of the previous call */
    fun recognize(image: RgbaImage): List<HandGesture> = landmarker.detect(image).map { gesture(it, image.width, image.height) }

    /** the hands in [image] and their gestures */
    fun recognize(image: ColorBuffer) = recognize(image.toRgbaImage())

    private fun gesture(hand: HandLandmarks, width: Int, height: Int): HandGesture {
        // like MediaPipe's LandmarksToMatrixCalculator: the landmarks in the coordinates of the longer side of the image,
        // the depth in those of its width, as MediaPipe's normalized landmarks have it
        val longest = max(width, height).toDouble()
        val image = hand.landmarks.map { Vector3(it.x / longest, it.y / longest, it.z / width) }
        val right = if (hand.rightHanded) hand.handednessScore else 1.0 - hand.handednessScore
        val embedding = embedder.run(
            mapOf(
                "hand" to (relativeToWrist(image) to longArrayOf(1, 21, 3)),
                "handedness" to (floats(listOf(right)) to longArrayOf(1, 1)),
                "world_hand" to (relativeToWrist(hand.worldLandmarks) to longArrayOf(1, 21, 3))
            )
        ).values.single()
        val scores = classifier.run(floats(embedding.map { it.toDouble() }), longArrayOf(1, embedding.size.toLong())).values.single()
        val best = scores.indices.maxBy { scores[it] }
        return HandGesture(hand, GESTURES[best], scores[best].toDouble(), GESTURES.zip(scores.map { it.toDouble() }).toMap())
    }

    /** the landmarks relative to the wrist, divided by the largest extent of x and y */
    private fun relativeToWrist(landmarks: List<Vector3>): FloatBuffer {
        val origin = landmarks[0]
        val relative = landmarks.map { it - origin }
        val scale = max(relative.maxOf { it.x } - relative.minOf { it.x }, relative.maxOf { it.y } - relative.minOf { it.y }) + 1E-5
        return floats(relative.flatMap { listOf(it.x / scale, it.y / scale, it.z / scale) })
    }

    private fun floats(values: List<Double>): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (v in values) buffer.put(v.toFloat())
        buffer.rewind()
        return buffer
    }

    override fun close() {
        landmarker.close()
        embedder.close()
        classifier.close()
    }
}

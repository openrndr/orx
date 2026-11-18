import org.openrndr.extra.mediapipe.FaceDetector
import org.openrndr.extra.mediapipe.HandLandmarker
import org.openrndr.extra.mediapipe.RgbaImage
import org.openrndr.math.Vector2
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The results of the models against those of MediaPipe 0.10 in Python, with the models and images that
 * `tools/convert_models.py` downloads; the tests pass without checking anything where they are missing
 */
class TestMediaPipe {
    private val models = File("models")

    private fun image(name: String): RgbaImage {
        val image = ImageIO.read(File(models, "images/$name"))
        val pixels = ByteArray(image.width * image.height * 4)
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val argb = image.getRGB(x, y)
            val o = (y * image.width + x) * 4
            pixels[o] = (argb shr 16).toByte()
            pixels[o + 1] = (argb shr 8).toByte()
            pixels[o + 2] = argb.toByte()
            pixels[o + 3] = (argb ushr 24).toByte()
        }
        return RgbaImage(image.width, image.height, pixels)
    }

    private fun assertNear(expected: Vector2, actual: Vector2, tolerance: Double, what: String) =
        assertTrue(expected.distanceTo(actual) < tolerance, "$what at $actual, MediaPipe has $expected")

    @Test
    fun `a face, its box and keypoints`() {
        if (!File(models, "blaze_face_short_range.onnx").exists()) return
        FaceDetector(File(models, "blaze_face_short_range.onnx").path).use { detector ->
            val face = detector.detect(image("portrait.jpg")).single()
            assertEquals(0.922, face.score, 0.01)
            // MediaPipe rounds the box to whole pixels
            assertNear(Vector2(283.0, 115.0), face.bounds.corner, 2.0, "the corner")
            assertEquals(234.0, face.bounds.width, 2.0)
            val keypoints = listOf(
                Vector2(363.4, 183.6), Vector2(460.0, 184.4), Vector2(420.7, 241.1),
                Vector2(417.9, 285.0), Vector2(295.7, 200.0), Vector2(503.0, 198.8)
            )
            for ((expected, actual) in keypoints.zip(face.keypoints)) assertNear(expected, actual, 1.0, "a keypoint")
        }
    }

    @Test
    fun `hands, their landmarks and handedness`() {
        if (!File(models, "hand_detector.onnx").exists()) return
        HandLandmarker(File(models, "hand_detector.onnx").path, File(models, "hand_landmarks_detector.onnx").path).use { landmarker ->
            // the wrist, the tip of the thumb and the tip of the index finger, and the depth of the index finger; the
            // landmarks are on average 0.7 to 3 percent of the size of the hand from those of MediaPipe, whose crops
            // around the palms could not be reproduced exactly, the faces are found as MediaPipe finds them
            class Expected(val score: Double, val wrist: Vector2, val thumb: Vector2, val index: Vector2, val indexDepth: Double)
            val expected = mapOf(
                "thumb_up.jpg" to listOf(Expected(0.984, Vector2(244.0, 272.6), Vector2(180.9, 101.9), Vector2(183.6, 210.6), -0.1102 * 382)),
                "pointing_up.jpg" to listOf(Expected(0.995, Vector2(171.6, 279.2), Vector2(158.6, 186.1), Vector2(169.6, 73.7), -0.1509 * 358)),
                "right_hands.jpg" to listOf(
                    Expected(0.943, Vector2(582.9, 353.2), Vector2(394.3, 204.4), Vector2(463.5, 51.5), -0.1193 * 720),
                    Expected(0.960, Vector2(137.2, 27.6), Vector2(325.1, 177.1), Vector2(257.5, 328.4), -0.1261 * 720)
                )
            )
            for ((name, hands) in expected) {
                val found = landmarker.detect(image(name))
                assertEquals(hands.size, found.size, name)
                for (e in hands) {
                    // in the order of MediaPipe or not
                    val hand = found.minBy { it.landmarks[0].xy.distanceTo(e.wrist) }
                    assertTrue(hand.rightHanded, name)
                    assertEquals(e.score, hand.handednessScore, 0.04, name)
                    assertNear(e.wrist, hand.landmarks[0].xy, 12.0, "$name: the wrist")
                    assertNear(e.thumb, hand.landmarks[4].xy, 12.0, "$name: the thumb")
                    assertNear(e.index, hand.landmarks[8].xy, 12.0, "$name: the index finger")
                    // closer than the wrist; the depth changes most with the crop
                    assertEquals(e.indexDepth, hand.landmarks[8].z, 0.5 * kotlin.math.abs(e.indexDepth), name)
                }
            }
        }
    }

    @Test
    fun `hands are followed from frame to frame`() {
        if (!File(models, "hand_detector.onnx").exists()) return
        HandLandmarker(File(models, "hand_detector.onnx").path, File(models, "hand_landmarks_detector.onnx").path, tracking = true).use { landmarker ->
            val image = image("right_hands.jpg")
            val first = landmarker.detect(image)
            assertEquals(2, first.size)
            // the two hands are followed, from the squares around their landmarks instead of their palms
            repeat(3) {
                val next = landmarker.detect(image)
                assertEquals(2, next.size)
                for (hand in first) {
                    val same = next.minBy { it.landmarks[0].xy.distanceTo(hand.landmarks[0].xy) }
                    val mean = hand.landmarks.zip(same.landmarks).sumOf { (a, b) -> a.xy.distanceTo(b.xy) } / 21.0
                    assertTrue(mean < 10.0, "followed $mean pixels from where it was found")
                }
            }
            // in a frame without hands they are lost, and found again
            assertTrue(landmarker.detect(RgbaImage(image.width, image.height, ByteArray(image.width * image.height * 4))).isEmpty())
            assertEquals(2, landmarker.detect(image).size)
        }
    }
}

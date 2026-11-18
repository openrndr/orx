import org.openrndr.extra.mediapipe.FaceDetector
import org.openrndr.extra.mediapipe.FaceLandmarker
import org.openrndr.extra.mediapipe.FaceLandmarks
import org.openrndr.extra.mediapipe.HandLandmarker
import org.openrndr.extra.mediapipe.ImageSegmenter
import org.openrndr.extra.mediapipe.InteractiveSegmenter
import org.openrndr.extra.mediapipe.PoseLandmarker
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

    @Test
    fun `a body, its landmarks and their visibility, with the full and the lite model`() {
        if (!File(models, "pose_detector.onnx").exists()) return
        // the nose, the wrists and the ankles, and the depth of the left wrist
        class Expected(val points: Map<Int, Vector2>, val leftWristDepth: Double)
        val expected = mapOf(
            "full" to Expected(
                mapOf(0 to Vector2(462.9, 286.2), 15 to Vector2(695.2, 315.1), 16 to Vector2(301.9, 308.0), 27 to Vector2(700.9, 615.9), 28 to Vector2(351.0, 607.7)),
                -231.8
            ),
            "lite" to Expected(
                mapOf(0 to Vector2(464.9, 282.0), 15 to Vector2(697.6, 313.0), 16 to Vector2(298.8, 310.4), 27 to Vector2(699.3, 612.1), 28 to Vector2(350.4, 607.6)),
                -234.3
            )
        )
        for ((variant, e) in expected) {
            PoseLandmarker(File(models, "pose_detector.onnx").path, File(models, "pose_landmarks_detector_$variant.onnx").path).use { landmarker ->
                val pose = landmarker.detect(image("pose.jpg")).single()
                assertEquals(33, pose.landmarks.size)
                for ((i, point) in e.points) assertNear(point, pose.landmarks[i].xy, 3.0, "$variant: landmark $i")
                // the depth changes most with the crop
                assertEquals(e.leftWristDepth, pose.landmarks[15].z, 0.2 * kotlin.math.abs(e.leftWristDepth), variant)
                for (i in e.points.keys) assertTrue(pose.visibility[i] > 0.95, "$variant: landmark $i is visible")
            }
        }
    }

    @Test
    fun `bodies are followed from frame to frame`() {
        if (!File(models, "pose_detector.onnx").exists()) return
        PoseLandmarker(File(models, "pose_detector.onnx").path, File(models, "pose_landmarks_detector_full.onnx").path, tracking = true).use { landmarker ->
            val image = image("pose.jpg")
            val first = landmarker.detect(image).single()
            repeat(3) {
                val next = landmarker.detect(image).single()
                val mean = first.landmarks.zip(next.landmarks).sumOf { (a, b) -> a.xy.distanceTo(b.xy) } / 33.0
                assertTrue(mean < 10.0, "followed $mean pixels from where it was found")
            }
            assertTrue(landmarker.detect(RgbaImage(image.width, image.height, ByteArray(image.width * image.height * 4))).isEmpty())
            assertEquals(1, landmarker.detect(image).size)
        }
    }

    @Test
    fun `a face mesh, with the irises, and the blendshapes of the face`() {
        if (!File(models, "face_landmarks_detector.onnx").exists()) return
        FaceLandmarker(
            File(models, "blaze_face_short_range.onnx").path,
            File(models, "face_landmarks_detector.onnx").path,
            File(models, "face_blendshapes.onnx").path
        ).use { landmarker ->
            val face = landmarker.detect(image("portrait.jpg")).single()
            assertEquals(478, face.landmarks.size)
            // the tip of the nose, the outer corners of the eyes, the centers of the irises and the chin
            val expected = mapOf(
                1 to Vector2(407.2, 231.4), 33 to Vector2(348.0, 177.6), 263 to Vector2(464.6, 176.0),
                468 to Vector2(363.1, 175.1), 473 to Vector2(449.7, 174.4), 152 to Vector2(407.3, 347.7)
            )
            for ((i, point) in expected) assertNear(point, face.landmarks[i].xy, 0.5, "landmark $i")
            assertEquals(-52.3, face.landmarks[1].z, 0.5)
            // a smile
            assertEquals(52, face.blendshapes.size)
            assertEquals(0.959, face.blendshapes.getValue("mouthSmileLeft"), 0.01)
            assertEquals(0.934, face.blendshapes.getValue("mouthSmileRight"), 0.01)
            // the triangles of the mesh
            assertEquals(898, FaceLandmarks.TRIANGLES.size)
            assertTrue(FaceLandmarks.TRIANGLES.all { t -> t.size == 3 && t.all { it in 0 until 468 } })
        }
    }

    @Test
    fun `faces are followed from frame to frame`() {
        if (!File(models, "face_landmarks_detector.onnx").exists()) return
        FaceLandmarker(
            File(models, "blaze_face_short_range.onnx").path, File(models, "face_landmarks_detector.onnx").path, null, tracking = true
        ).use { landmarker ->
            val image = image("portrait.jpg")
            val first = landmarker.detect(image).single()
            assertTrue(first.blendshapes.isEmpty())
            repeat(3) {
                val next = landmarker.detect(image).single()
                val mean = first.landmarks.zip(next.landmarks).sumOf { (a, b) -> a.xy.distanceTo(b.xy) } / 478.0
                assertTrue(mean < 5.0, "followed $mean pixels from where it was found")
            }
            assertTrue(landmarker.detect(RgbaImage(image.width, image.height, ByteArray(image.width * image.height * 4))).isEmpty())
            assertEquals(1, landmarker.detect(image).size)
        }
    }

    @Test
    fun `the categories of the pixels of people, and people`() {
        if (!File(models, "selfie_multiclass_256x256.onnx").exists()) return
        ImageSegmenter(File(models, "selfie_multiclass_256x256.onnx").path).use { segmenter ->
            assertEquals(listOf("background", "hair", "body-skin", "face-skin", "clothes", "others"), segmenter.categories)
            val image = image("portrait.jpg")
            val segmentation = segmenter.segment(image)
            assertEquals(256, segmentation.width)
            fun at(category: String, x: Double, y: Double) = segmentation.confidenceAt(category, x, y, image.width, image.height)
            // the face, the background and the jacket, as MediaPipe finds them
            assertEquals(0.919, at("face-skin", 410.0, 250.0), 0.01)
            assertEquals(0.917, at("background", 60.0, 60.0), 0.01)
            assertEquals(0.917, at("clothes", 480.0, 800.0), 0.01)
            // the softmax of each pixel
            assertEquals(1.0, segmenter.categories.sumOf { at(it, 300.0, 300.0) }, 1E-4)
            val mask = segmentation.categoryMask()
            assertEquals(segmenter.categories.indexOf("face-skin"), mask[(250 * 256 / 1024) * 256 + 410 * 256 / 820])
        }
        for (model in listOf("selfie_segmenter", "selfie_segmenter_landscape")) {
            ImageSegmenter(File(models, "$model.onnx").path).use { segmenter ->
                assertEquals(listOf("person"), segmenter.categories)
                val image = image("pose.jpg")
                val segmentation = segmenter.segment(image)
                fun at(x: Double, y: Double) = segmentation.confidenceAt("person", x, y, image.width, image.height)
                // the body, the sky, and the edge of an arm
                assertEquals(1.0, at(480.0, 400.0), 0.01, model)
                assertEquals(0.0, at(100.0, 100.0), 0.01, model)
                assertEquals(0.13, at(700.0, 300.0), 0.03, model)
            }
        }
    }

    @Test
    fun `the object at a point or along a scribble`() {
        if (!File(models, "magic_touch.onnx").exists()) return
        InteractiveSegmenter(File(models, "magic_touch.onnx").path).use { segmenter ->
            // the person at the face, not the background, as MediaPipe finds them
            val portrait = image("portrait.jpg")
            val person = segmenter.segment(portrait, Vector2(410.0, 256.0))
            assertEquals(listOf("object"), person.categories)
            fun at(segmentation: org.openrndr.extra.mediapipe.Segmentation, image: RgbaImage, x: Double, y: Double) =
                segmentation.confidenceAt("object", x, y, image.width, image.height)
            assertEquals(1.0, at(person, portrait, 480.0, 800.0), 0.05)
            assertEquals(0.0, at(person, portrait, 60.0, 60.0), 0.05)

            // the hand at the point, not the other hand
            val hands = image("right_hands.jpg")
            val left = segmenter.segment(hands, Vector2(180.0, 191.0))
            assertEquals(1.0, at(left, hands, 180.0, 191.0), 0.05)
            assertEquals(0.0, at(left, hands, 540.0, 200.0), 0.05)
            // a scribble over the other hand selects it instead
            val right = segmenter.segment(hands, listOf(Vector2(520.0, 250.0), Vector2(560.0, 200.0), Vector2(580.0, 150.0)))
            assertTrue(at(right, hands, 540.0, 220.0) > 0.9)
            assertTrue(at(right, hands, 180.0, 191.0) < 0.1)
        }
    }
}

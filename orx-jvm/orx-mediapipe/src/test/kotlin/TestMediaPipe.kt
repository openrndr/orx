import org.openrndr.extra.mediapipe.FaceDetector
import org.openrndr.extra.mediapipe.FaceLandmarker
import org.openrndr.extra.mediapipe.FaceLandmarks
import org.openrndr.extra.mediapipe.FaceStylizer
import org.openrndr.extra.mediapipe.GestureRecognizer
import org.openrndr.extra.mediapipe.HandLandmarker
import org.openrndr.extra.mediapipe.HolisticLandmarker
import org.openrndr.extra.mediapipe.ImageClassifier
import org.openrndr.extra.mediapipe.ImageEmbedder
import org.openrndr.extra.mediapipe.ObjectDetector
import org.openrndr.extra.mediapipe.ImageSegmenter
import org.openrndr.extra.mediapipe.InteractiveSegmenter
import org.openrndr.extra.mediapipe.PoseLandmarker
import org.openrndr.extra.mediapipe.RgbaImage
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
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

    @Test
    fun `the gestures of hands`() {
        if (!File(models, "gesture_embedder.onnx").exists()) return
        GestureRecognizer(
            File(models, "hand_detector.onnx").path, File(models, "hand_landmarks_detector.onnx").path,
            File(models, "gesture_embedder.onnx").path, File(models, "canned_gesture_classifier.onnx").path
        ).use { recognizer ->
            // the gestures and their scores as MediaPipe finds them, by the wrists of the hands; the scores differ as
            // much as the landmarks of the hands differ from those of MediaPipe
            val expected = mapOf(
                "thumb_up.jpg" to listOf(Triple(Vector2(244.0, 272.6), "Thumb_Up", 0.744)),
                "pointing_up.jpg" to listOf(Triple(Vector2(171.6, 279.2), "Pointing_Up", 0.83)),
                "victory.jpg" to listOf(Triple(Vector2(176.6, 291.1), "Victory", 0.775)),
                "fist.jpg" to listOf(Triple(Vector2(170.8, 234.1), "Closed_Fist", 0.9)),
                "right_hands.jpg" to listOf(
                    Triple(Vector2(582.9, 353.2), "Open_Palm", 0.603),
                    Triple(Vector2(137.2, 27.6), "None", 0.591)
                )
            )
            for ((name, hands) in expected) {
                val gestures = recognizer.recognize(image(name))
                assertEquals(hands.size, gestures.size, name)
                for ((wrist, gesture, score) in hands) {
                    val found = gestures.minBy { it.hand.landmarks[0].xy.distanceTo(wrist) }
                    assertEquals(gesture, found.gesture, name)
                    assertEquals(score, found.score, 0.08, name)
                    assertEquals(1.0, found.scores.values.sum(), 1E-4)
                }
            }
        }
    }

    @Test
    fun `a stylized face`() {
        if (!File(models, "face_stylizer_color_sketch.onnx").exists()) return
        FaceStylizer(
            File(models, "face_stylizer_color_sketch.onnx").path,
            File(models, "face_stylizer_face_detector.onnx").path,
            File(models, "face_stylizer_landmarks_detector.onnx").path,
            seed = 1
        ).use { stylizer ->
            val face = stylizer.stylize(image("portrait.jpg"))!!
            // the square around the face, as MediaPipe's FaceToRectCalculator finds it
            assertEquals(Vector2(407.0, 186.0), face.center)
            assertEquals(336.0, face.size)
            assertEquals(0.0, face.rotation, 0.01)
            assertEquals(256, face.image.width)
            // the brightness of the stylized face in 4 × 4 blocks, as MediaPipe stylizes it; the noise of the model
            // changes them by less than a level
            val expected = listOf(
                232.2, 160.2, 170.9, 235.4, 200.5, 193.7, 185.3, 197.2,
                200.1, 189.9, 183.4, 195.3, 179.6, 181.4, 172.0, 163.5
            )
            for (by in 0 until 4) for (bx in 0 until 4) {
                var sum = 0.0
                for (y in by * 64 until (by + 1) * 64) for (x in bx * 64 until (bx + 1) * 64) {
                    val o = (y * 256 + x) * 4
                    sum += ((face.image.pixels[o].toInt() and 0xff) + (face.image.pixels[o + 1].toInt() and 0xff) + (face.image.pixels[o + 2].toInt() and 0xff)) / 3.0
                }
                assertEquals(expected[by * 4 + bx], sum / (64 * 64), 2.0, "block $bx, $by")
            }
            assertTrue(stylizer.stylize(RgbaImage(64, 64, ByteArray(64 * 64 * 4))) == null)
        }
    }

    @Test
    fun `the mask of a body, and the heavy pose model`() {
        if (!File(models, "pose_landmarks_detector_heavy.onnx").exists()) return
        for (variant in listOf("full", "heavy")) {
            PoseLandmarker(File(models, "pose_detector.onnx").path, File(models, "pose_landmarks_detector_$variant.onnx").path).use { landmarker ->
                val pose = landmarker.detect(image("pose.jpg")).single()
                // the body, the sky, an arm and a leg, as MediaPipe's mask has them
                assertEquals(1.0, pose.mask.at(480.0, 400.0), 0.02, variant)
                assertEquals(0.0, pose.mask.at(100.0, 100.0), 0.02, variant)
                assertEquals(1.0, pose.mask.at(560.0, 500.0), 0.02, variant)
                assertEquals(256, pose.mask.width)
                if (variant == "heavy") {
                    // the nose, the left wrist and the right ankle, as MediaPipe finds them
                    assertNear(Vector2(459.8, 285.4), pose.landmarks[0].xy, 3.0, "the nose")
                    assertNear(Vector2(699.5, 312.8), pose.landmarks[15].xy, 3.0, "the left wrist")
                    assertNear(Vector2(348.6, 608.5), pose.landmarks[28].xy, 3.0, "the right ankle")
                }
            }
        }
    }

    /** an image of the demo data of orx */
    private fun demoImage(name: String): RgbaImage {
        val file = File("../../demo-data/images/$name")
        return image(file.relativeTo(File(models, "images")).path)
    }

    @Test
    fun `the categories of images`() {
        if (!File(models, "efficientnet_lite0.onnx").exists()) return
        // the most likely categories and their scores as MediaPipe finds them; the scores differ by up to 0.02 from
        // those of MediaPipe, whose preprocessing could not be reproduced exactly
        val expected = mapOf(
            "efficientnet_lite0" to mapOf("vw-beetle.jpg" to ("beach wagon" to 0.341), "peopleCity01.jpg" to ("miniskirt" to 0.153)),
            "efficientnet_lite2" to mapOf("vw-beetle.jpg" to ("beach wagon" to 0.253), "peopleCity01.jpg" to ("traffic light" to 0.117))
        )
        for ((model, images) in expected) {
            ImageClassifier(File(models, "$model.onnx").path).use { classifier ->
                assertEquals(1000, classifier.labels.size)
                for ((name, best) in images) {
                    val categories = classifier.classify(demoImage(name), maxResults = 3)
                    assertEquals(best.first, categories.first().label, "$model, $name")
                    assertEquals(best.second, categories.first().score, 0.03, "$model, $name")
                    assertTrue(categories[0].score >= categories[1].score)
                }
            }
        }
    }

    @Test
    fun `the embeddings of images, and how alike they are`() {
        if (!File(models, "mobilenet_v3_small.onnx").exists()) return
        // the cosine similarities of the embeddings of MediaPipe
        val expected = mapOf(
            "mobilenet_v3_small" to listOf(0.072, 0.018, 0.04),
            "mobilenet_v3_large" to listOf(-0.007, 0.136, -0.014)
        )
        for ((model, similarities) in expected) {
            ImageEmbedder(File(models, "$model.onnx").path).use { embedder ->
                val (beetle, people, cover) = listOf("vw-beetle.jpg", "peopleCity01.jpg", "life-cover.jpg").map { embedder.embed(demoImage(it)) }
                assertEquals(1.0, ImageEmbedder.cosineSimilarity(beetle, beetle), 1E-6)
                val found = listOf(
                    ImageEmbedder.cosineSimilarity(beetle, people),
                    ImageEmbedder.cosineSimilarity(beetle, cover),
                    ImageEmbedder.cosineSimilarity(people, cover)
                )
                for ((e, f) in similarities.zip(found)) assertEquals(e, f, 0.02, model)
            }
        }
    }

    @Test
    fun `objects, their boxes and categories`() {
        if (!File(models, "efficientdet_lite0.onnx").exists()) return
        // the most likely objects as MediaPipe finds them
        val expected = mapOf(
            "efficientdet_lite0" to mapOf(
                "peopleCity01.jpg" to Triple("person", 0.743, Rectangle(42.0, 197.0, 92.0, 290.0)),
                "vw-beetle.jpg" to Triple("car", 0.544, Rectangle(8.0, 116.0, 291.0, 184.0))
            ),
            "ssd_mobilenet_v2" to mapOf(
                "peopleCity01.jpg" to Triple("person", 0.788, Rectangle(40.0, 195.0, 97.0, 290.0)),
                "vw-beetle.jpg" to Triple("car", 0.422, Rectangle(7.0, 108.0, 296.0, 189.0))
            )
        )
        for ((model, images) in expected) {
            ObjectDetector(File(models, "$model.onnx").path, scoreThreshold = 0.2).use { detector ->
                for ((name, best) in images) {
                    val objects = detector.detect(demoImage(name))
                    val first = objects.first()
                    assertEquals(best.first, first.category.label, "$model, $name")
                    assertEquals(best.second, first.category.score, 0.015, "$model, $name")
                    assertNear(best.third.corner, first.bounds.corner, 3.0, "$model, $name: the corner")
                    assertNear(best.third.corner + best.third.dimensions, first.bounds.corner + first.bounds.dimensions, 3.0, "$model, $name: the other corner")
                    assertTrue(objects.all { it.category.score >= 0.2 })
                }
                // as many as asked for
                detector.maxResults = 2
                assertEquals(2, detector.detect(demoImage("peopleCity01.jpg")).size)
            }
        }
    }

    @Test
    fun `hair`() {
        if (!File(models, "hair_segmenter.onnx").exists()) return
        ImageSegmenter(File(models, "hair_segmenter.onnx").path).use { segmenter ->
            assertEquals(listOf("background", "hair"), segmenter.categories)
            val image = image("portrait.jpg")
            val segmentation = segmenter.segment(image)
            // the top of the head and the face, as MediaPipe's mask has them
            assertEquals(1.0, segmentation.confidenceAt("hair", 400.0, 60.0, image.width, image.height), 0.05)
            assertEquals(0.0, segmentation.confidenceAt("hair", 410.0, 250.0, image.width, image.height), 0.05)
        }
    }

    @Test
    fun `a person, the body, the face and the hands`() {
        if (!File(models, "hand_roi_refinement.onnx").exists()) return
        HolisticLandmarker(
            File(models, "holistic_pose_detector.onnx").path, File(models, "holistic_pose_landmarks_detector.onnx").path,
            File(models, "face_stylizer_face_detector.onnx").path, File(models, "face_stylizer_landmarks_detector.onnx").path,
            File(models, "face_blendshapes.onnx").path, File(models, "holistic_hand_landmarks_detector.onnx").path,
            File(models, "hand_roi_refinement.onnx").path
        ).use { holistic ->
            val person = holistic.detect(image("male_full_height_hands.jpg"))!!
            // as MediaPipe finds them: the nose and the wrists of the body, the tip of the nose of the face, the wrists
            // and the tips of the index fingers of the hands
            assertNear(Vector2(328.8, 162.8), person.pose.landmarks[0].xy, 3.0, "the nose of the body")
            assertNear(Vector2(376.1, 392.1), person.pose.landmarks[15].xy, 3.0, "the left wrist of the body")
            assertNear(Vector2(152.2, 500.5), person.pose.landmarks[16].xy, 3.0, "the right wrist of the body")
            val face = person.face!!
            assertEquals(478, face.landmarks.size)
            assertNear(Vector2(329.3, 162.0), face.landmarks[1].xy, 3.0, "the tip of the nose")
            assertEquals(52, face.blendshapes.size)
            val left = person.leftHand!!
            val right = person.rightHand!!
            assertTrue(!left.rightHanded && right.rightHanded)
            assertNear(Vector2(384.8, 396.5), left.landmarks[0].xy, 2.0, "the left wrist")
            assertNear(Vector2(296.5, 387.5), left.landmarks[8].xy, 2.0, "the left index finger")
            assertNear(Vector2(157.2, 497.2), right.landmarks[0].xy, 2.0, "the right wrist")
            assertNear(Vector2(95.6, 549.6), right.landmarks[8].xy, 2.0, "the right index finger")
            // the wrists of the hands at those of the body in the world
            assertEquals(person.pose.worldLandmarks[15], left.worldLandmarks[0])
        }
    }
}

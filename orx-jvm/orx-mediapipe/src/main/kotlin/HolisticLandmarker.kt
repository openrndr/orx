package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A person found by [HolisticLandmarker]: the body, and the face and the hands of the body where they are found
 * @param leftHand the left hand of the person, null where it is not found
 * @param rightHand the right hand of the person, null where it is not found
 */
class HolisticLandmarks(val pose: PoseLandmarks, val face: FaceLandmarks?, val leftHand: HandLandmarks?, val rightHand: HandLandmarks?)

/**
 * Finds a person like MediaPipe's holistic landmarker: the body is found by a [PoseLandmarker], the face and the hands
 * in squares around the landmarks of the face and the hands of the body: the face by the face detector in a square
 * around the landmarks of the face of the body, the hands in squares around the wrists, the pinkies and the index
 * fingers of the body, refined by a model of their own. The hands are those of the body, left and right as the person
 * has them. With [tracking] the body is followed from frame to frame.
 * @param blendshapesPath the blendshapes model, or null to find no blendshapes
 * @param minHandVisibility the least visibility of a wrist of the body to look for its hand
 */
class HolisticLandmarker(
    poseDetectorPath: String = "orx-jvm/orx-mediapipe/models/holistic_pose_detector.onnx",
    poseLandmarksPath: String = "orx-jvm/orx-mediapipe/models/holistic_pose_landmarks_detector.onnx",
    faceDetectorPath: String = "orx-jvm/orx-mediapipe/models/face_stylizer_face_detector.onnx",
    faceLandmarksPath: String = "orx-jvm/orx-mediapipe/models/face_stylizer_landmarks_detector.onnx",
    blendshapesPath: String? = "orx-jvm/orx-mediapipe/models/face_blendshapes.onnx",
    handLandmarksPath: String = "orx-jvm/orx-mediapipe/models/holistic_hand_landmarks_detector.onnx",
    handRefinementPath: String = "orx-jvm/orx-mediapipe/models/hand_roi_refinement.onnx",
    var minHandVisibility: Double = 0.1,
    tracking: Boolean = false
) : AutoCloseable {
    /** the landmarker of the body */
    val poseLandmarker = PoseLandmarker(poseDetectorPath, poseLandmarksPath, maxPoses = 1, tracking = tracking)
    private val faceDetector = FaceDetector(faceDetectorPath)
    private val faceLandmarker = FaceLandmarker(faceDetectorPath, faceLandmarksPath, blendshapesPath)
    // the palms are not looked for, the hands are found from the body
    private val handLandmarker = HandLandmarker(landmarksPath = handLandmarksPath)
    private val handRefinement = OnnxModel(handRefinementPath)

    /** true to follow the body in the frames of a video */
    var tracking: Boolean
        get() = poseLandmarker.tracking
        set(value) {
            poseLandmarker.tracking = value
        }

    /** the person in [image], null without a body */
    fun detect(image: RgbaImage): HolisticLandmarks? {
        val pose = poseLandmarker.detect(image).firstOrNull() ?: return null
        val points = pose.landmarks.map { it.xy }
        return HolisticLandmarks(
            pose,
            face(image, points),
            hand(image, pose, wrist = 15, pinky = 17, index = 19, right = false),
            hand(image, pose, wrist = 16, pinky = 18, index = 20, right = true)
        )
    }

    /** the person in [image], null without a body */
    fun detect(image: ColorBuffer) = detect(image.toRgbaImage())

    /**
     * The face: the face detector in a square around the landmarks of the face of the body, three times as large, turned
     * such that the eyes are level, then the landmarks in a square around the eyes of the face that is found
     */
    private fun face(image: RgbaImage, pose: List<Vector2>): FaceLandmarks? {
        val face = pose.take(11)
        val box = bounds(face)
        val fromPose = square(box.center, max(box.width, box.height) * 3.0, rotationTo(face[5], face[2], 0.0))
        val detection = faceDetector.detectIn(image, fromPose).firstOrNull() ?: return null
        // the bounds of the keypoints, twice as large, turned like the eyes
        val keypoints = bounds(detection.keypoints)
        val fromDetection = square(keypoints.center, max(keypoints.width, keypoints.height) * 2.0,
            rotationTo(detection.keypoints[0], detection.keypoints[1], 0.0))
        return faceLandmarker.landmarks(image, fromDetection)
    }

    /**
     * The hand at the wrist of the body: a square around the wrist, the pinky and the index finger of the body, refined
     * by the model of the region of the hand, in which the landmarks of the hand are found
     */
    private fun hand(image: RgbaImage, pose: PoseLandmarks, wrist: Int, pinky: Int, index: Int, right: Boolean): HandLandmarks? {
        if (pose.visibility[wrist] <= minHandVisibility) return null
        val w = pose.landmarks[wrist].xy
        // like MediaPipe's HandDetectionsFromPoseToRectsCalculator: two thirds from the pinky to the index finger
        val middle = (pose.landmarks[index].xy * 2.0 + pose.landmarks[pinky].xy) / 3.0
        val fromPose = shiftedSquare(middle, middle.distanceTo(w) * 2.0, uprightRotation(w, middle), 2.7)

        // the model of the region of the hand finds two points, the square is around the first, twice as large as
        // their distance, turned from the first to the second downwards
        val size = handRefinement.width
        val toImage = Affine.rotatedRect(fromPose, size)
        val raw = handRefinement.run(image.tensor(size, size, toImage, 0f, 1f, replicate = true)).values.single()
        val a = toImage(Vector2(raw[0].toDouble(), raw[1].toDouble()))
        val b = toImage(Vector2(raw[2].toDouble(), raw[3].toDouble()))
        val refined = shiftedSquare(a, a.distanceTo(b) * 2.0, rotationTo(a, b, -PI / 2.0), 1.0)

        val hand = handLandmarker.landmarks(image, refined) ?: return null
        // the hand of the side of the body, the world landmarks at the wrist of the body
        val offset = pose.worldLandmarks[wrist] - hand.worldLandmarks[0]
        return HandLandmarks(hand.landmarks, hand.worldLandmarks.map { it + offset }, right, 1.0, hand.score)
    }

    /** a square of [side] around [center], [scale] times as large and a tenth of [side] towards the fingers */
    private fun shiftedSquare(center: Vector2, side: Double, rotation: Double, scale: Double): RotatedRect {
        val shiftY = -0.1
        val shifted = center + Vector2(-side * shiftY * sin(rotation), side * shiftY * cos(rotation))
        return square(shifted, side * scale, rotation)
    }

    override fun close() {
        poseLandmarker.close()
        faceDetector.close()
        faceLandmarker.close()
        handLandmarker.close()
        handRefinement.close()
    }
}

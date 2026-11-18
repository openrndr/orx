# orx-mediapipe

Finds faces and the landmarks of hands with the models of [MediaPipe](https://ai.google.dev/edge/mediapipe/solutions/guide),
on the JVM with [ONNX Runtime](https://onnxruntime.ai). The models are converted from TFLite to ONNX, the pre- and
post-processing of MediaPipe's tasks is done in Kotlin.

## Models

The models are not part of orx. `tools/convert_models.py` downloads them, and the images of MediaPipe that the tests
use, and converts them to ONNX, into `orx-jvm/orx-mediapipe/models`:

```
uv run orx-jvm/orx-mediapipe/tools/convert_models.py
```

The models of MediaPipe are under the Apache License 2.0.

## Tasks

`FaceDetector` finds faces, their boxes and six keypoints, with the BlazeFace short range model, for faces within about
two meters of the camera. It finds the faces as MediaPipe's face detector finds them.

```kotlin
val detector = FaceDetector()
val faces = detector.detect(colorBuffer)
```

`HandLandmarker` finds hands, their 21 landmarks in the image and in meters, and whether they are left or right hands.
The palms are found in the whole image, the landmarks in a square around each palm. The landmarks are on average 0.7 to
3 percent of the size of the hand from those of MediaPipe.

```kotlin
val landmarker = HandLandmarker()
val hands = landmarker.detect(colorBuffer)
```

For video, `HandLandmarker(tracking = true)` follows the hands from frame to frame, around where their landmarks were in
the frame before, and looks for palms only while fewer than `maxHands` hands are followed. With two hands in a frame of
1280 × 720 pixels it takes about 8 ms on the CPU of an M-series Mac, about 24 ms without tracking.

Both read the pixels of a `ColorBuffer`, or take an `RgbaImage`.

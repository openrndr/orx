# orx-mediapipe

Finds faces, the landmarks of faces, hands and bodies, and segments people and objects, with the models of [MediaPipe](https://ai.google.dev/edge/mediapipe/solutions/guide),
on the JVM with [ONNX Runtime](https://onnxruntime.ai). The models are converted from TFLite to ONNX, the pre- and
post-processing of MediaPipe's tasks is done in Kotlin.

## Models

The models are not part of orx. `tools/convert_models.py` downloads them, and the images of MediaPipe that the tests
use, and converts them to ONNX, into `orx-jvm/orx-mediapipe/models`. The sparse weights of the pose detector and the
custom transpose convolutions of the selfie segmenters, which tf2onnx cannot read, are rewritten first.

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

`FaceLandmarker` finds the face mesh: 478 landmarks, the 468 of the mesh and 5 of each iris, and the 52 blendshapes of
the face, how much it shows each expression, like those of ARKit. `FaceLandmarks.TRIANGLES` are the triangles of the
mesh, `FaceLandmarks.CONTOURS` the lips, eyes, eyebrows, irises and the oval of the face. The landmarks are within 0.2
pixel of those of MediaPipe. With `tracking = true` the faces are followed from frame to frame; a face in a frame of
1280 × 720 pixels takes about 4 ms with its blendshapes, 9 ms without tracking.

```kotlin
val landmarker = FaceLandmarker(tracking = true)
val faces = landmarker.detect(colorBuffer)
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

`PoseLandmarker` finds bodies, BlazePose: their 33 landmarks in the image and in meters, and how likely each landmark is
to be visible. The bodies are found in the whole image, the landmarks in a square around each body, refined by the
heatmaps of the model. `pose_landmarks_detector_full.onnx` is used by default, `pose_landmarks_detector_lite.onnx` is
faster and less precise. The landmarks are on average within 1.1 pixel of those of MediaPipe, of a body of 340 pixels.
With `tracking = true` the bodies are followed from frame to frame like hands; a body in a frame of 1280 × 720 pixels
takes about 5 ms, 14 ms without tracking.

```kotlin
val landmarker = PoseLandmarker(tracking = true)
val poses = landmarker.detect(colorBuffer)
```

`ImageSegmenter` segments images with MediaPipe's selfie segmentation models. `selfie_multiclass_256x256.onnx`, the
default, finds hair, the skin of the face and of the body, clothes, others and the background, in about 25 ms;
`selfie_segmenter.onnx` and `selfie_segmenter_landscape.onnx`, for images of 16:9, find people in about 1 to 2 ms. The
masks are within 0.0002 of those of MediaPipe on average. `Segmentation.write` writes a mask into a color buffer, to
draw over the image.

```kotlin
val segmenter = ImageSegmenter()
val segmentation = segmenter.segment(colorBuffer)
segmentation.write("hair", mask)
```

`InteractiveSegmenter`, MagicTouch, segments the object at a point or along a scribble, in about 19 ms. The masks are
within 0.001 to 0.007 of those of MediaPipe on average, more than 99 percent of the pixels are in or out alike.

```kotlin
val segmenter = InteractiveSegmenter()
val segmentation = segmenter.segment(colorBuffer, mouse.position)
segmentation.write("object", mask)
```

All read the pixels of a `ColorBuffer`, or take an `RgbaImage`.

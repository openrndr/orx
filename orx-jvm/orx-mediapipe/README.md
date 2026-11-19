# orx-mediapipe

Finds faces, objects, the landmarks of faces, hands and bodies, classifies and segments images, with the models of [MediaPipe](https://ai.google.dev/edge/mediapipe/solutions/guide),
on the JVM with [ONNX Runtime](https://onnxruntime.ai). The models are converted from TFLite to ONNX, the pre- and
post-processing of MediaPipe's tasks is done in Kotlin.

## Models

The models are not part of orx. They are in the repository [orx-models](https://github.com/openrndr/orx-models), in
`mediapipe/onnx`, with the test images of MediaPipe in `mediapipe/images`. `MediaPipeModels` downloads the models
that are used, by default, from the tag `mediapipe-1` the first time, and keeps them in `~/.cache/orx-models`, or in
the folder of the system property `orx.models.cache`.

```kotlin
val hands = HandLandmarker()                                    // downloads hand_detector.onnx, hand_landmarks_detector.onnx
val image = loadImage(MediaPipeModels.image("thumb_up.jpg"))    // a test image of MediaPipe
```

`tools/convert_models.py` makes the models of orx-models: it downloads the models of MediaPipe and the images that the
tests use, and converts them to ONNX, into `orx-jvm/orx-mediapipe/models`, where the tests read them. What tf2onnx
cannot read is rewritten first: the sparse weights of the pose detector, the custom transpose convolutions of the
selfie segmenters, and the variables, the custom batch normalizations and the random noise of the face stylizers,
whose noise becomes inputs of the models, the custom pools with indices of the hair segmenter, and the resizes of
EfficientDet-Lite0, whose scales make outputs of the wrong size.

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
faster and less precise, `pose_landmarks_detector_heavy.onnx` slower and more precise. `PoseLandmarks.mask` is how
likely the pixels around the body are to be of the body, within 0.002 of MediaPipe's mask on average. The landmarks are on average within 1.1 pixel of those of MediaPipe, of a body of 340 pixels.
With `tracking = true` the bodies are followed from frame to frame like hands; a body in a frame of 1280 × 720 pixels
takes about 5 ms, 14 ms without tracking.

```kotlin
val landmarker = PoseLandmarker(tracking = true)
val poses = landmarker.detect(colorBuffer)
```

`ImageSegmenter` segments images with MediaPipe's selfie segmentation models and its hair segmenter. `selfie_multiclass_256x256.onnx`, the
default, finds hair, the skin of the face and of the body, clothes, others and the background, in about 25 ms;
`selfie_segmenter.onnx` and `selfie_segmenter_landscape.onnx`, for images of 16:9, find people in about 1 to 2 ms. The
masks are within 0.0002 of those of MediaPipe on average. `hair_segmenter.onnx` finds hair in about 16 ms; its masks are
within 0.02 of MediaPipe's on average, less close, and like MediaPipe's they find hair in more than hair. `Segmentation.write` writes a mask into a color buffer, to
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

`GestureRecognizer` recognizes the gestures of hands: `Closed_Fist`, `Open_Palm`, `Pointing_Up`, `Thumb_Down`,
`Thumb_Up`, `Victory` and `ILoveYou`, or `None`, from the landmarks of a `HandLandmarker`, with or without tracking. It
finds the gestures of MediaPipe, their scores differ as much as the landmarks of the hands do.

```kotlin
val recognizer = GestureRecognizer(tracking = true)
val gestures = recognizer.recognize(colorBuffer)
```

`FaceStylizer` turns the face in an image into a color sketch, color ink or an oil painting, with the models
`face_stylizer_color_sketch.onnx`, `face_stylizer_color_ink.onnx` and `face_stylizer_oil_painting.onnx`. It cuts the face
out of the image in a square around its eyes and mouth and gives the stylized square of 256 × 256 pixels, and where it
was in the image. The stylized faces are within a level of those of MediaPipe on average, as close as those of
MediaPipe are to each other, whose noise is random. It takes about 125 ms.

```kotlin
val stylizer = FaceStylizer("orx-jvm/orx-mediapipe/models/face_stylizer_oil_painting.onnx")
val face = stylizer.stylize(colorBuffer)
```

`ObjectDetector` finds objects of the 80 categories of COCO, their boxes and categories, with
`efficientdet_lite0.onnx` (13 ms), the more precise `efficientdet_lite2.onnx` (30 ms) or `ssd_mobilenet_v2.onnx`
(8 ms). The anchors of the models are read from their metadata by `tools/convert_models.py`. It finds the objects
of MediaPipe, with scores within 0.01; MediaPipe finds objects of all scores, `scoreThreshold` is 0.3 by default.

```kotlin
val detector = ObjectDetector()
val objects = detector.detect(colorBuffer)
```

`ImageClassifier` classifies images into the 1000 categories of ImageNet with `efficientnet_lite0.onnx` (5 ms) or
`efficientnet_lite2.onnx` (8 ms), with the categories of MediaPipe and scores within 0.03. `ImageEmbedder` gives
embeddings of images with `mobilenet_v3_small.onnx` or `mobilenet_v3_large.onnx` (2 to 3 ms), whose cosine
similarities say how much images look alike.

```kotlin
val categories = ImageClassifier().classify(colorBuffer)
val embedding = ImageEmbedder().embed(colorBuffer)
```

`HolisticLandmarker` finds a person: the body, the face mesh and the hands, the face and the hands in squares around
the landmarks of the body, the hands refined by a model of their own, left and right as the person has them. Its
landmarks are within 1 pixel of those of MediaPipe on average; it takes about 34 ms with tracking.

```kotlin
val holistic = HolisticLandmarker(tracking = true)
val person = holistic.detect(colorBuffer)
```

All read the pixels of a `ColorBuffer`, or take an `RgbaImage`.

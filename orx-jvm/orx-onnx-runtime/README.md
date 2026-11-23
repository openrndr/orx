# orx-onnx-runtime

Runs models in [ONNX](https://onnx.ai) with [ONNX Runtime](https://onnxruntime.ai), on the CPU or, on macOS, with
CoreML. `OnnxModel` loads a model, gives the shapes of its inputs and outputs by name, and runs it on tensors of
floats.

```kotlin
val model = OnnxModel("model.onnx", ExecutionProvider.CORE_ML)
val outputs = model.run(FloatTensor(floatArrayOf(0f, 1f, 2f), longArrayOf(3)))
val output = outputs.getValue("output").toFloatArray()
```

[orx-mediapipe](../orx-mediapipe) and [orx-onnx-models](../orx-onnx-models) run their models with it.

# orx-onnx-models

Models in ONNX for orx, run by [orx-onnx-runtime](../orx-onnx-runtime).

## Variational autoencoders

`VAETrainer` trains a variational autoencoder on vectors, in PyTorch in a virtual environment of Python that it makes
with [orx-python](../orx-python), and exports its encoder and decoder to ONNX. `VAEEncoderModel` maps vectors to
embeddings of a few values, `VAEDecoderModel` maps embeddings back to vectors, to explore the space of the data.

```kotlin
val (encoderFile, decoderFile) = VAETrainer().train(File("vae-colors"), colors, latentDimensions = 2)
val encoder = VAEEncoderModel(encoderFile.path)
val decoder = VAEDecoderModel(decoderFile.path)
val embeddings = encoder.encode(colors)
val decoded = decoder.decode(embeddings)
```

`test-data` holds an encoder and a decoder trained on random colors.

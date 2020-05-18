# orx-time-operators

A collection of time-sensitive functions aimed at controlling raw data over-time, 
such as Envelope and LFO.

## Usage

Use the TimeOperators extension to `tick` the operators, making them advance in time.

```kotlin
extend(TimeOperators()) {
    track(envelope, lfo)
}
```

### Envelope

```kotlin
val size = Envelope(50.0, 400.0, 0.5, 0.5)

if (frameCount % 80 == 0) {
    size.trigger() // also accepts a new target value
}

drawer.circle(0.0, 0.0, size.value)
```

### LFO

```kotlin
val size = LFO(LFOWave.Sine) // default LFOWave.Saw

val freq = 0.5
val phase = 0.5

drawer.circle(0.0, 0.0, size.sample(freq, phase))

// or

drawer.circle(0.0, 0.0, size.sine(freq, phase))
```


<!-- __demos__ >
# Demos
[DemoEnvelopeKt](src/demo/kotlin/DemoEnvelopeKt.kt
![DemoEnvelopeKt](https://github.com/openrndr/orx/blob/media/orx-time-operators/images/DemoEnvelopeKt.png
[DemoLFOKt](src/demo/kotlin/DemoLFOKt.kt
![DemoLFOKt](https://github.com/openrndr/orx/blob/media/orx-time-operators/images/DemoLFOKt.png
<!-- __demos__ -->
## Demos
### DemoEnvelope
[source code](src/demo/kotlin/DemoEnvelope.kt)

![DemoEnvelopeKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-time-operators/images/DemoEnvelopeKt.png)

### DemoLFO
[source code](src/demo/kotlin/DemoLFO.kt)

![DemoLFOKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-time-operators/images/DemoLFOKt.png)

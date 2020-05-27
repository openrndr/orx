# orx-time-operators

A collection of time-sensitive functions aimed at controlling raw data over-time, 
such as Envelope and LFO.

For more detailed information, read: [An introduction to orx-time-operators](https://openrndr.discourse.group/t/an-introduction-to-orx-time-operators/108)

## Usage

Use the TimeOperators extension to `tick` the operators, making them advance in time.

```kotlin
extend(TimeOperators()) {
    track(envelope, lfo)
}
```

### Envelope

An Attack/Decay based envelope which takes into account the elapsed time to change a given value over time. It runs through two phases, `Attack` and `Decay` which can be changed to shape the output values.

```kotlin
val size = Envelope(50.0, 400.0, 0.5, 0.5)

if (frameCount % 80 == 0) {
    size.trigger() // also accepts a new target value
}

drawer.circle(0.0, 0.0, size.value)
```

### LFO

Generates oscillating waves between `[0.0, 1.0]` tied to the frame rate.

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

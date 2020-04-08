# orx-time-operators

A collection of time-sensitive functions aimed at controlling raw data over-time.

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
val size = LFO(LFOWave.SINE) // default LFOWave.SAW

val freq = 0.5
val phase = 0.5

drawer.circle(0.0, 0.0, size.sample(freq, phase))

// or

drawer.circle(0.0, 0.0, size.sine(freq, phase))
```



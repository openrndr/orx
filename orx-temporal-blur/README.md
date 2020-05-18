# orx-temporal-blur

Post-processing temporal-blur video effect. CPU intense, therefore not intended 
for use with the `ScreenRecorder` extension or other real-time uses.

This extension uses multi-sampling to accumulate and average the final image. 

Multi-sampling is performed by modifying `Program.clock` 
while processing the tail-end of the extension chain. This multi-sampling strategy is slow and not
entirely suited in real-time and/or interactive settings.

`orx-temporal-blur` works well with programs that use `seconds` for their animation input.
This includes `Animatables`, but only after the `Animatable` clock is synchronized with the `Program` clock.
(Which you should already have done when using `ScreenRecorder`) 

Synchronizing clocks in OPENRNDR 0.3.36 (current release):
```
Animatable.clock(object: Clock {
                override val time: Long
                    get() = (clock() * 1E3).toLong()
            })
```

Synchronizing high precision clocks in OPENRNDR 0.3.37 (future release)
```
Animatable.clock(object: Clock {
                override val time: Long
                get() = timeNanos / 1000
                override val timeNanos: Long
                    get() = (clock() * 1E6).toLong()

            })
```

Note that time-step-based simulations or integrations will likely break because your drawing code will be executed multiple times
per frame.

## Configuration

```kotlin
extend(TemporalBlur()) {
    duration = 0.9
    samples = 30
    fps = 60.0 
    jitter = 1.0
}
```

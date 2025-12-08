# orx-temporal-blur

Post-processing temporal-blur video effect. CPU intense, not intended for real-time uses.

If using `ScreenRecorder` make sure to enable the `frameClock` and `frameRate` in its properties.
This will make `seconds` be based on the current frame count instead of the real time.

This extension uses multi-sampling to accumulate and average the final image. 

Multi-sampling is performed by modifying `Program.clock` 
while processing the tail-end of the extension chain. This multi-sampling strategy is slow and not
entirely suited in real-time and/or interactive settings.

`orx-temporal-blur` works well with programs that use `seconds` for their animation input.

Note that time-step-based simulations or integrations will likely break because your drawing code will be executed multiple times
per frame.

## Configuration

```kotlin
extend(TemporalBlur()) {
    duration = 0.9 // duration is in frames
    samples = 30 
    fps = 60.0 
    jitter = 1.0
}
```

## Color shifts

Additionally, a color matrix can be set per accumulation step. See [`DemoColorShift01.kt`](src/demo/kotlin/DemoColorShift01.kt)

```kotlin
extend(TemporalBlur()) {
    colorMatrix = {
        // `it` is 0.0 at start of frame, 1.0 at end of frame
        tint(ColorRGBa.WHITE.mix(ColorRGBa.BLUE, it))
    }
}
```


<!-- __demos__ -->
## Demos
### DemoBasic01

A minimal demonstration of `TemporalBlur`.

Each animation frame will be rendered 10 times
sampling and mixing 0.9 seconds of animation.


![DemoBasic01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-temporal-blur/images/DemoBasic01Kt.png)

[source code](src/demo/kotlin/DemoBasic01.kt)

### DemoColorShift01

A more advance demonstration of `TemporalBlur`,
showing how a function can be used to apply a varying
`colorMatrix` transformation across the sampling
duration.

This example makes the samples fade
from white to blue in each frame.

The `gain` parameter is used to increase the brightness
of the resulting pixels.

![DemoColorShift01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-temporal-blur/images/DemoColorShift01Kt.png)

[source code](src/demo/kotlin/DemoColorShift01.kt)

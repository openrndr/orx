# orx-easing

Provides easing functions for smooth animation or non-linear interpolation.

Similar to those on https://easings.net

 - `easeLinear`
 - `easeBackIn`, `easeBackInOut`, `easeBackOut`
 - `easeBounceIn`, `easeBounceInOut`, `easeBounceOut`
 - `easeCircIn`, `easeCircInOut`, `easeCircOut`
 - `easeCubicIn`, `easeCubicInOut` `easeCubicOut`
 - `easeElasticIn`, `easeElasticInOut`, `easeElasticOut`
 - `easeExpoIn`, `easeExpoInOut`, `easeExpoOut`
 - `easeQuadIn`, `easeQuadInOut`, `easeQuadOut`
 - `easeQuartIn`, `easeQuartInOut`, `easeQuartOut`
 - `easeQuintIn`, `easeQuintInOut`, `easeQuintOut`
 - `easeSineIn`, `easeSineInOut`, `easeSineOut`

## usage

`fun easeX(time: Double, bias: Double = 0.0, scale: Double = 1.0, duration : Double = 1.0)`

```kotlin
// -- when t is in [0, 1]
val et = easeQuadIn(t)
val et = easeQuadIn(t, 0.0, 1.0, 10.0)
```

Using the `Easing` enumeration

```kotlin
val et = Easing.QuadIn.function(t, 0.0, 1.0, 1.0)
```

<!-- __demos__ -->
## Demos
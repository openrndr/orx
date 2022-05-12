# orx-easing

Provides easing functions for smooth animation or non-linear interpolation.

Similar to those on https://easings.net

|       linear | constant 0 | constant 1 |
|-------------:|-----------:|-----------:|
| `easeLinear` | `easeZero` |  `easeOne` |

|              in |             in out |              out | exponent |
|----------------:|-------------------:|-----------------:|:--------:|
|    `easeQuadIn` |    `easeQuadInOut` |    `easeQuadOut` |    2     |
|   `easeCubicIn` |   `easeCubicInOut` |   `easeCubicOut` |    3     |
|   `easeQuartIn` |   `easeQuartInOut` |   `easeQuartOut` |    4     |
|   `easeQuintIn` |   `easeQuintInOut` |   `easeQuintOut` |    5     |
|    `easeCircIn` |    `easeCircInOut` |    `easeCircOut` |          |
|    `easeExpoIn` |    `easeExpoInOut` |    `easeExpoOut` |          |
|    `easeSineIn` |    `easeSineInOut` |    `easeSineOut` |          |
|    `easeBackIn` |    `easeBackInOut` |    `easeBackOut` |          |
|  `easeBounceIn` |  `easeBounceInOut` |  `easeBounceOut` |          |
| `easeElasticIn` | `easeElasticInOut` | `easeElasticOut` |          |

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
### DemoEasings01
[source code](src/demo/kotlin/DemoEasings01.kt)

![DemoEasings01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-easing/images/DemoEasings01Kt.png)

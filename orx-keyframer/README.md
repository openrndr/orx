# orx-keyframer

Create animated timelines by specifying properties and times in keyframes, 
then play it back at any speed (even backwards) automatically interpolating properties. 
Save, load, use mathematical expressions and callbacks. Powerful and highly reusable.

What this allows you to do:

1. Create a keyframed animation in a json file.

```json
[
  {
    "time": 0.0,
    "easing": "cubic-in-out",
    "x": 3.0,
    "y": 4.0,
    "z": 9.0,
    "r": 0.1,
    "g": 0.5,
    "b": 0.2,
    "radius": 50
  },
  {
    "time": 2.0,
    "easing": "cubic-in-out",
    "r": 0.6,
    "g": 0.5,
    "b": 0.1
  },
  {
    "time": 4.0,
    "easing": "cubic-in-out",
    "x": 10.0,
    "y": 4.0,
    "radius": 400
  },
  {
    "time": 5.0,
    "easing": "cubic-in-out",
    "x": 100.0,
    "y": 320.0,
    "radius": 400
  },
  {
    "time": 5.3,
    "easing": "cubic-in-out",
    "x": 100.0,
    "y": 320.0,
    "radius": 40
  }
]
```

2. Map the animation data to Kotlin types:

```kotlin
class Animation : Keyframer() {
    val position by Vector2Channel(arrayOf("x", "y"))
    val radius by DoubleChannel("radius")
    val color by RGBChannel(arrayOf("r", "g", "b"))
}

val animation = Animation()
animation.loadFromJson(File("data/keyframes/animation.json"))
```

3. Animate! (from an OPENRNDR program)

```kotlin
extend {
    animation(seconds)
    drawer.fill = animation.color
    drawer.circle(animation.position, animation.radius)
}
```
## Easing

All the easing functions of orx-easing are available 

 - linear
 - back-in
 - back-out
 - back-in-out
 - bounce-in
 - bounce-out
 - bounce-in-out
 - circ-in
 - circ-out
 - circ-in-out
 - cubic-in
 - cubic-out
 - cubic-in-out
 - elastic-in
 - elastic-out
 - elastic-in-out
 - expo-in
 - expo-out
 - expo-in-out
 - quad-in
 - quad-out
 - quad-in-out
 - quart-in
 - quart-out
 - quart-in-out
 - quint-in
 - quint-out
 - quint-in-out
 - sine-in
 - sine-out
 - sine-in-out
 - one
 - zero


## Advanced features

orx-keyframer uses two file formats. A `SIMPLE` format and a `FULL` format. For reference check the [example full format .json](src/demo/resources/demo-full-01.json) and the [example program](src/demo/kotlin/DemoFull01.kt).
The full format adds a `parameters` block and a `prototypes` block.

[Repeats](src/demo/resources/demo-simple-repetitions-01.json), simple key repeating mechanism

[Expressions](src/demo/resources/demo-simple-expressions-01.json), expression mechanism. Currently uses values `r` to indicate repeat index and `t` the last used key time, `v` the last used value (for the animated attribute).

Supported functions in expressions:
 - `min(x, y)`, `max(x, y)`
 - `cos(x)`, `sin(x)`, `acos(x)`, `asin(x)`, `tan(x)`, `atan(x)`, `atan2(y, x)`
 - `abs(x)`, `saturate(x)`
 - `degrees(x)`, `radians(x)`
 - `pow(x,y)`, `sqrt(x)`, `exp(x)`
 - `mix(left, right, x)`
 - `smoothstep(t0, t1, x)`
 - `map(leftBefore, rightBefore, leftAfter, rightAfter, x)`
 - `random()`, `random(min, max)`
  
[Parameters and prototypes](src/demo/resources/demo-full-01.json)

<!-- __demos__ >
# Demos
[DemoFull01Kt](src/demo/kotlin/DemoFull01Kt.kt
![DemoFull01Kt](https://github.com/openrndr/orx/blob/media/orx-keyframer/images/DemoFull01Kt.png
[DemoScrub01Kt](src/demo/kotlin/DemoScrub01Kt.kt
![DemoScrub01Kt](https://github.com/openrndr/orx/blob/media/orx-keyframer/images/DemoScrub01Kt.png
[DemoSimple01Kt](src/demo/kotlin/DemoSimple01Kt.kt
![DemoSimple01Kt](https://github.com/openrndr/orx/blob/media/orx-keyframer/images/DemoSimple01Kt.png
[DemoSimple02Kt](src/demo/kotlin/DemoSimple02Kt.kt
![DemoSimple02Kt](https://github.com/openrndr/orx/blob/media/orx-keyframer/images/DemoSimple02Kt.png
[DemoSimpleExpressions01Kt](src/demo/kotlin/DemoSimpleExpressions01Kt.kt
![DemoSimpleExpressions01Kt](https://github.com/openrndr/orx/blob/media/orx-keyframer/images/DemoSimpleExpressions01Kt.png
[DemoSimpleRepetitions01Kt](src/demo/kotlin/DemoSimpleRepetitions01Kt.kt
![DemoSimpleRepetitions01Kt](https://github.com/openrndr/orx/blob/media/orx-keyframer/images/DemoSimpleRepetitions01Kt.png
<!-- __demos__ -->
## Demos
### DemoFull01
[source code](src/demo/kotlin/DemoFull01.kt)

![DemoFull01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-keyframer/images/DemoFull01Kt.png)

### DemoScrub01
[source code](src/demo/kotlin/DemoScrub01.kt)

![DemoScrub01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-keyframer/images/DemoScrub01Kt.png)

### DemoSimple01
[source code](src/demo/kotlin/DemoSimple01.kt)

![DemoSimple01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-keyframer/images/DemoSimple01Kt.png)

### DemoSimple02
[source code](src/demo/kotlin/DemoSimple02.kt)

![DemoSimple02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-keyframer/images/DemoSimple02Kt.png)

### DemoSimpleExpressions01
[source code](src/demo/kotlin/DemoSimpleExpressions01.kt)

![DemoSimpleExpressions01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-keyframer/images/DemoSimpleExpressions01Kt.png)

### DemoSimpleRepetitions01
[source code](src/demo/kotlin/DemoSimpleRepetitions01.kt)

![DemoSimpleRepetitions01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-keyframer/images/DemoSimpleRepetitions01Kt.png)

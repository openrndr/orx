# orx-keyframer

Create animated timelines by specifying properties and times in keyframes, then play it back at any speed (even
backwards) automatically interpolating properties. Save, load, use mathematical expressions and callbacks. Powerful and
highly reusable.

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
    "radius": {
      "value": 50.0,
      "easing": "linear"
    }
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

## More expressive interface

orx-keyframer has two ways of programming key frames. The first is the `"x": <number>` style we have seen before. The
second way uses a dictionary instead of a number value.

For example:

```json
[
  {
    "time": 0.0,
    "x": 320.0,
    "y": 240.0
  },
  {
    "time": 10.0,
    "easing": "cubic-out",
    "x": {
      "easing": "cubic-in-out",
      "value": 0.0
    },
    "y": {
      "duration": -5.0,
      "easing": "cubic-in",
      "value": 0.0
    }
  },
  {
    "time": 20.0,
    "x": 640.0,
    "y": 480.0,
    "easing": "cubic-in-out"
  }
]
```

Inside the value dictionary one can set `value`, `easing`, `duration` and `envelope`.

 * `value` the target value, required value
 * `easing` easing method that overrides the key's easing method, optional value
 * `duration` an optional duration for the animation, set to `0` to jump from the previous
value to the new value, a negative value will start the interpolation before `time`. A positive value
   wil start the interpolation at `time` and end at `time + duration`
* `envelope` optional 2-point envelope that modifies the playback of the animation. The default envelope is
`[0.0, 1.0]`. Reverse playback is achieved by supplying `[1.0, 0.0]`. To start the animation later try `[0.1, 1.0]`,
  to end the animation earlier try `[0.0, 0.9]`
  
## Advanced features

orx-keyframer uses two file formats. A `SIMPLE` format and a `FULL` format. For reference check
the [example full format .json](src/demo/resources/demo-full-01.json) and
the [example program](src/demo/kotlin/DemoFull01.kt). The full format adds a `parameters` block and a `prototypes`
block.

[Expressions](src/demo/resources/demo-simple-expressions-01.json), expression mechanism. Currently uses values `r` to
indicate repeat index and `t` the last used key time, `v` the last used value (for the animated attribute).

Supported functions in expressions:

- `min(x, y)`, `max(x, y)`
- `cos(x)`, `sin(x)`, `acos(x)`, `asin(x)`, `tan(x)`, `atan(x)`, `atan2(y, x)`
- `abs(x)`, `saturate(x)`
- `degrees(x)`, `radians(x)`
- `pow(x, y)`, `sqrt(x)`, `exp(x)`
- `mix(left, right, x)`
- `smoothstep(t0, t1, x)`
- `map(leftBefore, rightBefore, leftAfter, rightAfter, x)`
- `random()`, `random(min, max)`

[Parameters and prototypes](src/demo/resources/demo-full-01.json)

<!-- __demos__ -->
## Demos
### DemoEvelope01
[source code](src/demo/kotlin/DemoEvelope01.kt)

![DemoEvelope01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-keyframer/images/DemoEvelope01Kt.png)

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

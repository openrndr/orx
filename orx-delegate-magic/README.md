# orx-delegate magic

Collection of magical property delegators. For tracking variable change or
interpolate towards the value of a variable.

## Delegated properties

[Kotlin documentation](https://kotlinlang.org/docs/delegated-properties.html)

## Property smoothing

```kotlin
val state = object {
    var radius = 10.0
}

val smoothRadius by smoothing(state::radius)
```


## Property dynamics

```kotlin
val state = object {
    var radius = 10.0
}

val dynamicRadius by springForcing(state::radius)
```

## Property tracking

```kotlin
val state = object {
    var radius = 10.0
}

val radiusHistory by tracking(state::radius)
```
<!-- __demos__ -->
## Demos
### DemoDifferencing01



![DemoDifferencing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoDifferencing01Kt.png)

[source code](src/jvmDemo/kotlin/DemoDifferencing01.kt)

### DemoFollowing01

Demonstrates using delegate-magic tools with
[Double] and [Vector2].

The white circle's position uses [following].
The red circle's position uses [smoothing].

`following` uses physics (velocity and acceleration).
`smoothing` eases values towards the target.

Variables using delegates (`by`) interpolate
toward target values, shown as gray lines.

The behavior of the delegate-magic functions can be configured
via arguments that affect their output.

The arguments come in pairs of similar name:
The first one, often of type [Double], is constant,
The second one contains `Property` in its name and can be
modified after its creation and even be linked to a UI
to modify the behavior of the delegate function in real time.
The `Property` argument overrides the other.

![DemoFollowing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoFollowing01Kt.png)

[source code](src/jvmDemo/kotlin/DemoFollowing01.kt)

### DemoSmoothing01



![DemoSmoothing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoSmoothing01Kt.png)

[source code](src/jvmDemo/kotlin/DemoSmoothing01.kt)

### DemoSpring01



![DemoSpring01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoSpring01Kt.png)

[source code](src/jvmDemo/kotlin/DemoSpring01.kt)

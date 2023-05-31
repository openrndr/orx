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
[source code](src/jvmDemo/kotlin/DemoDifferencing01.kt)

![DemoDifferencing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoDifferencing01Kt.png)

### DemoFollowing01
[source code](src/jvmDemo/kotlin/DemoFollowing01.kt)

![DemoFollowing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoFollowing01Kt.png)

### DemoSmoothing01
[source code](src/jvmDemo/kotlin/DemoSmoothing01.kt)

![DemoSmoothing01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoSmoothing01Kt.png)

### DemoSpring01
[source code](src/jvmDemo/kotlin/DemoSpring01.kt)

![DemoSpring01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-delegate-magic/images/DemoSpring01Kt.png)

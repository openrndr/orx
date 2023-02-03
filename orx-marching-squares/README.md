# orx-marching-squares

Tools for extracting contours from functions

## How to use it?

`orx-marching-squares` provides the `findContours()` function

```kotlin
fun f(v: Vector2) = v.distanceTo(drawer.bounds.center) - 200.0
val contours = findContours(::f, drawer.bounds, 16.0)
drawer.contours(contours)
```

With a small adjustment to the given function one can use `findContours` to find iso contours. The trick is to add a cosine over the distance function.

```kotlin
fun f(v: Vector2) = cos((v.distanceTo(drawer.bounds.center) / 100.0) * 2 * PI)
val contours = findContours(::f, drawer.bounds.offsetEdges(32.0), 16.0)
drawer.contours(contours)
```
<!-- __demos__ -->
## Demos
### FindContours01
[source code](src/jvmDemo/kotlin/FindContours01.kt)

![FindContours01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-marching-squares/images/FindContours01Kt.png)

### FindContours02
[source code](src/jvmDemo/kotlin/FindContours02.kt)

![FindContours02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-marching-squares/images/FindContours02Kt.png)

### FindContours03
[source code](src/jvmDemo/kotlin/FindContours03.kt)

![FindContours03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-marching-squares/images/FindContours03Kt.png)

### FindContours04
[source code](src/jvmDemo/kotlin/FindContours04.kt)

![FindContours04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-marching-squares/images/FindContours04Kt.png)

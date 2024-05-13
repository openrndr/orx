# orx-hash-grid

2D space partitioning for fast point queries.

## Usage

`orx-hash-grid` provides the classes `HashGrid` and `Cell`, in most cases only `HashGrid` is used.

Create a hash grid for a given radius. 
```kotlin
val grid = HashGrid(radius)
```

Check for a given query point if the grid is free, i.e. there is no point in the grid at distance less than `radius` away from the
query point.

```kotlin
grid.isFree(query)
```

Add a point to the hash grid structure: 
```kotlin
grid.insert(point)
```

Iterate over all points in the hash grid:
```kotlin 
for (point in grid.points()) {
    // do something with point
}
```

## Extensions to standard library

`orx-hash-grid` provides short-hand extension functions to `List<Vector2>`

<hr>

```kotlin
fun List<Vector2>.filter(radius: Double) : List<Vector2>
 ``` 

filters the points in the list such that only points with an inter-distance of `radius` remain.

```kotlin
val points = (0 until 10_000).map { drawer.bounds.uniform() }
val filtered = points.filter(20.0)
```

<hr>

```kotlin 
fun List<Vector2>.hashGrid(radius: Double) : HashGrid
```
constructs a (mutable) `HashGrid` containing all points in the list.

```kotlin
val points = (0 until 10_000).map { drawer.bounds.uniform() }
val hashGrid = points.hashGrid(20.0)
```

<hr>

## References

 * `orx-noise` uses `HashGrid` to generate Poisson distributed points. [Link](https://github.com/openrndr/orx/blob/master/orx-noise/src/commonMain/kotlin/PoissonDisk.kt)

<!-- __demos__ -->
## Demos
### DemoFilter01
[source code](src/jvmDemo/kotlin/DemoFilter01.kt)

![DemoFilter01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoFilter01Kt.png)

### DemoFilter3D01
[source code](src/jvmDemo/kotlin/DemoFilter3D01.kt)

![DemoFilter3D01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoFilter3D01Kt.png)

### DemoHashGrid01
[source code](src/jvmDemo/kotlin/DemoHashGrid01.kt)

![DemoHashGrid01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoHashGrid01Kt.png)

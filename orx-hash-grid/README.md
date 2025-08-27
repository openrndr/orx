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


The program performs the following steps:
- Generates 10,000 random points uniformly distributed within the drawable bounds.
- Filters the generated points to enforce a minimum distance of 20.0 units between them.
- Visualizes the filtered points as circles with a radius of 10.0 units on the canvas.
![DemoFilter01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoFilter01Kt.png)

[source code](src/jvmDemo/kotlin/DemoFilter01.kt)

### DemoFilter3D01

This demo sets up and renders a 3D visualization of filtered random points displayed as small spheres.

The program performs the following key steps:
- Generates 10,000 random 3D points within a ring defined by a minimum and maximum radius.
- Filters the points to ensure a minimum distance between any two points using a spatial hash grid.
- Creates a small sphere mesh that will be instanced for each filtered point.
- Sets up an orbital camera to allow viewing the 3D scene interactively.
- Renders the filtered points by translating the sphere mesh to each point's position and applying a shader that modifies the fragment color based on the view normal.
![DemoFilter3D01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoFilter3D01Kt.png)

[source code](src/jvmDemo/kotlin/DemoFilter3D01.kt)

### DemoHashGrid01

This demo sets up an interactive graphics application with a configurable
display window and visualization logic. It uses a `HashGrid` to manage points
in a 2D space and randomly generates points within the drawable area. These
points are then inserted into the grid if they satisfy certain spatial conditions.
The visual output includes:
- Rectangles representing the bounds of the cells in the grid.
- Circles representing the generated points.
![DemoHashGrid01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoHashGrid01Kt.png)

[source code](src/jvmDemo/kotlin/DemoHashGrid01.kt)

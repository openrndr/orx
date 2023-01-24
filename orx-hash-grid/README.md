# orx-hash-grid

A 2D space partitioning for points.

## Usage

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
<!-- __demos__ -->
## Demos
### DemoFilter01
[source code](src/jvmDemo/kotlin/DemoFilter01.kt)

![DemoFilter01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoFilter01Kt.png)

### DemoHashGrid01
[source code](src/jvmDemo/kotlin/DemoHashGrid01.kt)

![DemoHashGrid01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-hash-grid/images/DemoHashGrid01Kt.png)

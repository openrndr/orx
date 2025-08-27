# orx-quadtree

A [Quadtree](https://en.wikipedia.org/wiki/Quadtree) is a spatial
partioning tree structure meant to provide fast spatial queries such as nearest points within a range.

## Example

```kotlin
val box = Rectangle.fromCenter(Vector2(400.0), 750.0)

val quadTree = Quadtree<Vector2>(box) { it }

for (point in points) {
    quadTree.insert(point)
}

val nearestQuery = quadTree.nearest(points[4], 20.0)
```

### Author

Ricardo Matias / [@ricardomatias](https://github.com/ricardomatias)
<!-- __demos__ -->
## Demos
### DemoQuadTree01



![DemoQuadTree01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-quadtree/images/DemoQuadTree01Kt.png)

[source code](src/jvmDemo/kotlin/DemoQuadTree01.kt)

### DemoQuadTree02



![DemoQuadTree02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-quadtree/images/DemoQuadTree02Kt.png)

[source code](src/jvmDemo/kotlin/DemoQuadTree02.kt)

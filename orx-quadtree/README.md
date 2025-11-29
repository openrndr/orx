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

Demonstrates how to create a `QuadTree` data structure,
how to add 2D points to it, and how to visualize all the quads
created for the current set of points.

The demo creates 1000 points using a Gaussian distribution, which
creates a higher density of points in the center of the window.

The `QuadTree` algorithm tries to keep the number of points
per quad balanced, which in this case leads to larger quads
near the edges of the window, and small quads at the center.

![DemoQuadTree01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-quadtree/images/DemoQuadTree01Kt.png)

[source code](src/jvmDemo/kotlin/DemoQuadTree01.kt)

### DemoQuadTree02

This demo shows how to query `QuadTree` points within a given radius
from a given point, using the `QuadTree.nearest()` method.

It also demonstrates how to iterate over the data returned by
`.nearest()`, including the `nearest` point, the `neighbours` points,
and the quads with borders within or touching the requested radius.


![DemoQuadTree02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-quadtree/images/DemoQuadTree02Kt.png)

[source code](src/jvmDemo/kotlin/DemoQuadTree02.kt)

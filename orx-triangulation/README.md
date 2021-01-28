# orx-triangulation

An extension for triangulating a set of points using the **Delaunay** triangulation method. From that triangulation we can also derive a **Voronoi** diagram.

The functionality comes from a Javascript port of the following libraries:

* [delaunator](https://github.com/ricardomatias/delaunator) (external)
* [d3-delaunay](https://github.com/d3/d3-delaunay) (the port is included in this package)

## Usage

### Delaunay

The entry point is the `Delaunay` class.

```kotlin
    val points: List<Vector2>
    val delaunay = Delaunay.from(points)

    // or
    val flatPoints: DoubleArray // (x0, y0, x1, x1, x2, y2)
    val delaunay = Delaunay(flatPoints)
```

This is how you retrieve the triangulation results:

```kotlin
val triangles: List<Triangle> = delaunay.triangles()
val halfedges: List<ShapeContour> = delaunay.halfedges()
val hull: ShapeContour = delaunay.hull()

// Updates the triangulation after the points have been modified in-place.
delaunay.update()
```

### Voronoi

The bounds specifices where the Voronoi diagram will be clipped.

```kotlin
val bounds: Rectangle

val delaunay = Delaunay.from(points)
val voronoi = delaunay.voronoi(bounds)
// or
val voronoi = Voronoi(Delaunay.from(points), bounds)
```

See [To Infinity and Back Again](https://observablehq.com/@mbostock/to-infinity-and-back-again) for an interactive explanation of Voronoi cell clipping.

This is how you retrieve th results:

```kotlin
val cells: List<ShapeContour> = voronoi.cellsPolygons()
val cell: ShapeContour = voronoi.cellPolygon(int) // index
val circumcenters: List<Vector2> = voronoi.circumcenters()

// Returns true if the cell with the specified index i contains the specified vector
val contaisVector = voronoi.contains(int, Vector2)

// Updates the Voronoi diagram and underlying triangulation
// after the points have been modified in-place
voronoi.update()
```


### Author

Ricardo Matias / [@ricardomatias](https://github.com/ricardomatias)
<!-- __demos__ -->
## Demos
### DemoDelaunay01
[source code](src/demo/kotlin/DemoDelaunay01.kt)

![DemoDelaunay01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoDelaunay01Kt.png)

### DemoDelaunay02
[source code](src/demo/kotlin/DemoDelaunay02.kt)

![DemoDelaunay02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoDelaunay02Kt.png)

### DemoVoronoi01
[source code](src/demo/kotlin/DemoVoronoi01.kt)

![DemoVoronoi01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi01Kt.png)

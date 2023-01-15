# orx-triangulation

An extension for triangulating a set of points using the **Delaunay** triangulation method. From that triangulation we can also derive a **Voronoi** diagram.

The functionality comes from a Javascript port of the following libraries:

* [delaunator](https://github.com/ricardomatias/delaunator) (external)
* [d3-delaunay](https://github.com/d3/d3-delaunay) (the port is included in this package)

## Usage

### DelaunayTriangulation

The entry point is the `DelaunayTriangulation` class.

```kotlin
    val points: List<Vector2>
    val delaunay = DelaunayTriangulation(points)

    // or
    
    val delaunay = points.delaunayTriangulation()
```

This is how you retrieve the triangulation results:

```kotlin
val triangles: List<Triangle> = delaunay.triangles()
val halfedges: List<ShapeContour> = delaunay.halfedges()
val hull: ShapeContour = delaunay.hull()

```

### Voronoi

The bounds specify where the Voronoi diagram will be clipped.

```kotlin
val bounds: Rectangle

val delaunay = points.delaunayTriangulation()
val voronoi = delaunay.voronoiDiagram(bounds)
// or
val voronoi = points.voronoiDiagram(bounds)
```

See [To Infinity and Back Again](https://observablehq.com/@mbostock/to-infinity-and-back-again) for an interactive explanation of Voronoi cell clipping.

This is how you retrieve th results:

```kotlin
val cells: List<ShapeContour> = voronoi.cellPolygons()
val cell: ShapeContour = voronoi.cellPolygon(int) // index
val circumcenters: List<Vector2> = voronoi.circumcenters

// Returns true if the cell with the specified index i contains the specified vector
val containsVector = voronoi.contains(int, Vector2)
```


### Authors

Ricardo Matias / [@ricardomatias](https://github.com/ricardomatias)
Edwin Jakobs / [@edwinRNDR](https://github.com/edwinRNDR)
<!-- __demos__ -->
## Demos
### DemoDelaunay01
[source code](src/jvmDemo/kotlin/DemoDelaunay01.kt)

![DemoDelaunay01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoDelaunay01Kt.png)

### DemoDelaunay02
[source code](src/jvmDemo/kotlin/DemoDelaunay02.kt)

![DemoDelaunay02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoDelaunay02Kt.png)

### DemoVoronoi01
[source code](src/jvmDemo/kotlin/DemoVoronoi01.kt)

![DemoVoronoi01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi01Kt.png)

### DemoVoronoi02
[source code](src/jvmDemo/kotlin/DemoVoronoi02.kt)

![DemoVoronoi02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi02Kt.png)

### DemoVoronoi03
[source code](src/jvmDemo/kotlin/DemoVoronoi03.kt)

![DemoVoronoi03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi03Kt.png)

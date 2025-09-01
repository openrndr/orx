# orx-triangulation

**Delaunay** triangulation and **Voronoi** diagrams.

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

This method sets up a graphical application using the OPENRNDR framework
to visually demonstrate Delaunay triangulation on a set of points scattered
along a circle with Poisson disk sampling.

The application features the following:
- A central circle with a defined radius.
- Points generated within the circle using a scatter algorithm that
maintains specific spacing and avoids clustering.
- Delaunay triangulation computed from the combined point set.
- Rendering of triangles that are part of the Delaunay triangulation.
- Visual styling with dynamic color shading for better clarity of layers
and triangle order.

This method demonstrates concepts of computational geometry and procedural
rendering.

![DemoDelaunay01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoDelaunay01Kt.png)

[source code](src/jvmDemo/kotlin/DemoDelaunay01.kt)

### DemoDelaunay02



![DemoDelaunay02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoDelaunay02Kt.png)

[source code](src/jvmDemo/kotlin/DemoDelaunay02.kt)

### DemoVoronoi01

This program generates a Voronoi diagram within a defined circular area and visualizes it.

The program performs the following:
- Defines a circular area and a rectangular bounding frame within the canvas.
- Uses Poisson Disk Sampling to generate points within the circular area.
- Computes the Delaunay triangulation for the generated points, including equidistant points on the circle boundary.
- Derives the Voronoi diagram using the Delaunay triangulation and the bounding frame.
- Extracts the cell polygons of the Voronoi diagram.
- Renders the Voronoi cell polygons on the canvas, with a pink stroke on a black background.

![DemoVoronoi01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi01Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi01.kt)

### DemoVoronoi02



![DemoVoronoi02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi02Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi02.kt)

### DemoVoronoi03



![DemoVoronoi03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi03Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi03.kt)

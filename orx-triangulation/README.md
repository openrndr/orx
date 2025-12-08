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

This demo shows how to use Delaunay triangulation to convert a Shape into a list of triangular ShapeContours.

The program starts by creating a Circle, then creates two sets of points:
- Points generated within the circle using a scatter algorithm that
maintains specific spacing and avoids clustering.
- Points sampled along the contour of the circle.

The `delaunayTriangulation()` method is called on the combined point set.
Next, it queries the resulting triangles and converts them into ShapeContour
instances.

Finally, it renders the triangles assigning unique fill and stroke colors
based on the triangle's index.

This method demonstrates concepts of computational geometry and procedural
rendering.

![DemoDelaunay01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoDelaunay01Kt.png)

[source code](src/jvmDemo/kotlin/DemoDelaunay01.kt)

### DemoDelaunay02

Demonstrates the `DelaunayTriangulation.halfedges()` method,
which returns the boundaries between the triangles in the set.

Commented out one can also discover the `hull()` method,
which returns a ShapeContour of a convex hull containing
all the points in the set.

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

A demo rendering four layers including a Voronoi diagram and a Delaunay triangulation,
producing a complex pattern.

A 8x8 grid of rectangles is produced, leaving a 50 pixel margin around the bounds of the window.
Those rectangles are mapped to circles, and each circle contour sampled in 6 locations.
This is the set of points used for the Delaunay triangulation.

Next, the four layers are rendered:

1. A white dot for each point in the set.
2. Pink contours for the Delaunay half edges.
3. Yellow contours with a Voronoi diagram discarding the ones touching the edges
4. Gray contours with the Delaunay triangles.

The structure is recalculated on every animation frame, making it easy
to animate some of the parameters.

![DemoVoronoi02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi02Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi02.kt)

### DemoVoronoi03

A variation of DemoVoronoi02.kt, also rendering four layers including
a Voronoi diagram and a Delaunay triangulation,
producing a complex pattern.

A 3x6 grid of rectangles is produced, leaving a 100 pixel margin around the bounds of the window.
Those rectangles are mapped to circles, and each circle contour sampled in 16 locations.
This is the set of points used for the Delaunay triangulation.

Next, four layers are rendered:

1. A white dot for each point in the set.
2. Pink contours for the Delaunay half edges.
3. A Voronoi diagram with yellow contours with translucent fill.
4. Gray contours with the Delaunay triangles.

The structure is recalculated on every animation frame, making it easy
to animate some of the parameters. Try replacing the 0.0 rotation
of the circles by other values or even `seconds` and observe what happens.

![DemoVoronoi03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-triangulation/images/DemoVoronoi03Kt.png)

[source code](src/jvmDemo/kotlin/DemoVoronoi03.kt)

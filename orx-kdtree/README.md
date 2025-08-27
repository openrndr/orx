# orx-kdtree

Fast search of points closest to the queried point in a data set. 2D, 3D and 4D.

<!-- __demos__ -->
## Demos
### DemoKNearestNeighbour01

This demo initializes an interactive graphical application where 1000 randomly distributed points
are displayed on a 2D canvas. A KD-tree structure is used for spatial querying of the points, enabling
efficient nearest-neighbor searches based on the user's cursor position. The closest 7 points to the
cursor are highlighted with circles and lines connecting them to the cursor.

Key features:
- Generates 1000 random 2D points within the canvas.
- Builds a KD-tree from the list of points for optimized spatial querying.
- Visualizes the points and highlights the 7 nearest neighbors to the user's cursor position dynamically.
- Highlights include red-colored circles around the nearest points and red lines connecting them to the cursor.
![DemoKNearestNeighbour01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-kdtree/images/DemoKNearestNeighbour01Kt.png)

[source code](src/jvmDemo/kotlin/DemoKNearestNeighbour01.kt)

### DemoNearestNeighbour01

Initializes an interactive graphical application that displays 1000 randomly distributed 2D points.
The points are organized into a KD-tree for efficient spatial querying.

Key functionality:
- Displays the points as small circles on the canvas.
- Dynamically highlights the nearest point to the cursor's position by drawing a larger circle around it.

Highlights:
- KD-tree structure enables efficient nearest-neighbor searches.
- The nearest point to the cursor is determined and visually emphasized in real-time as the cursor moves.
![DemoNearestNeighbour01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-kdtree/images/DemoNearestNeighbour01Kt.png)

[source code](src/jvmDemo/kotlin/DemoNearestNeighbour01.kt)

### DemoRangeQuery01

Initializes an interactive graphical application that demonstrates spatial querying with KD-trees.
A canvas is populated with 1000 randomly distributed 2D points, and a KD-tree is used for efficient
spatial operations. The program dynamically highlights points within a specified radius from the
user's cursor position.

Key features:
- Generates and displays 1000 random 2D points within the canvas.
- Builds a KD-tree structure for optimized querying of spatial data.
- Dynamically highlights points within a specified radius (50.0) from the cursor position.
- Visualizes the current query radius around the cursor as an outline circle.
- Uses different fill and stroke styles to distinguish highlighted points and query visuals.
![DemoRangeQuery01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-kdtree/images/DemoRangeQuery01Kt.png)

[source code](src/jvmDemo/kotlin/DemoRangeQuery01.kt)

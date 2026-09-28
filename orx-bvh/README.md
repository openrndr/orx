# orx-bvh

Bounding Volume Hierarchy: a tree structure used to speed up ray/geometry intersection tests. 

It wraps groups of geometric objects in progressively larger bounding volumes, so you can test the simpler boxes first and only test the detailed geometry when needed.


<!-- __demos__ -->
## Demos
### DemoBipartiteIntersections01



![DemoBipartiteIntersections01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-bvh/images/DemoBipartiteIntersections01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBipartiteIntersections01.kt)

### DemoIntersections01



![DemoIntersections01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-bvh/images/DemoIntersections01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoIntersections01.kt)

### DemoRectangleQuery01



![DemoRectangleQuery01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-bvh/images/DemoRectangleQuery01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoRectangleQuery01.kt)

# orx-force-2d

2D extended position-based dynamics (XPBD) for simulating physical forces. 

<!-- __demos__ -->
## Demos
### DemoBendyStar01

Demonstrates how to set up an interactive physics-driven scene with a 5-point star that is affected by gravity
and collides with a rectangular boundary. Instead of always pointing down, gravity points from the center
of the window towards the mouse position.

The simulation is instantiated via [ForceSimulation] (see the
[api docs](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-force-simulation/index.html))
We need to add bodies to the simulation. A way to create a body is by using [contourToBody]
[api docs](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/contour-to-body.html).

Note how when creating the body we enable `gravity` and three
constraints: link-length, body-area and rectangular-bounds.

`simulate()` is a suspending function, therefore we need to use `runBlocking`.
It takes two arguments: the time delta in milliseconds, and the number of
`substeps` to control the simulation precision.

The simulation engine does not actually work with contours or segments,
but with nodes instead. That's why for rendering we read the positions out
of the nodes and construct [LineSegment]s out of them.

![DemoBendyStar01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBendyStar01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBendyStar01.kt)

### DemoBendyStar02

A simulation similar to DemoBendyStar01.kt but with some
changed parameters and one additional constraint:
node-collision.

The four existing constraints have a `compliance` of 0.0
(its default value), meaning that all forces must be applied
with equal strictness.

In this case the regular star has 24 points,
`linkNeighbors` is reduced to 5 and the `iterations`
of the link-length constraint increased to 3, resulting
in a very different behavior.

![DemoBendyStar02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBendyStar02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBendyStar02.kt)

### DemoBendyStar03

This version of the BendyStar demo
uses a 10-point start and replaces.
`nodeCollisionConstraint` by `nodeRepulseForce`.

It also configures the coroutine context to enable
multithreading.

![DemoBendyStar03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBendyStar03Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBendyStar03.kt)

### DemoBendyStar04

A demo similar to DemoBendyStar03, but adding 4 bodies instead
of 1, and an interbody repulse force.

Two of the bodies are given a higher repulse force strength.
The low compliance values in the link-length and body-area constraints
indicate that those constraints must be strictly enforced.

Commented-out code can be enabled to visualize the nodes
and the body bounds.

![DemoBendyStar04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBendyStar04Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBendyStar04.kt)

### DemoBlob01

A demonstration with two soft body blobs.

The horizontal mouse position is used to divide the space in two adjacent
rectangles, each controlling the dimensions of a
[rectangularBoundsConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-rectangular-bounds-constraint/index.html)

Each blob lives inside those constraint rectangles.

A [bodyAreaConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-body-area-constraint/index.html)
"inflates" / "deflates" the blob to the specified `restArea`. In this example,
the vertical mouse position controls how much of the available constraint rectangle area
is used: 100% at the bottom and 0% at the bottom.

![DemoBlob01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBlob01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBlob01.kt)

### DemoBlob02

This soft body demo simulates the behavior
of 9 blobs, initially placed as circles on a
3x3 grid.

It uses the same forces and constraints as DemoBlob01:
gravity, rectangular-bounds, link-length and body-area
constraints.

[naiveBroadPhaseCollisionDetector](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/naive-broad-phase-collision-detector.html) and
[sapCollisionConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/sap-collision-constraint.html)
are required for the blobs to collider with each other.

The mouse position is used to control the direction of `gravity`.

If the points are initialized at random locations instead
of on a grid, blobs can randomly live inside other blobs.
Try uncommenting the `scatter` variant, or placing
a number of blobs all centered on the screen
and see how the simulation evolves.

![DemoBlob02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBlob02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBlob02.kt)

### DemoBlob03

A soft body simulation that begins with four stars placed
in a 2x2 grid and evolves into hard to predict outcomes
based on the parameters and their relation.

On top of a Gravity force, this one features a
[NodeRepulseForce](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-node-repulse-force/index.html)
and a
[NodeRepulseInterbodyForce](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/node-repulse-interbody-force.html).

![DemoBlob03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBlob03Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBlob03.kt)

### DemoBlobAndParticles01

Demonstration of how to include two types of bodies in the same simulation:
a blob and circular particles. It could be seen as a combination of one of
DemoBlob02 and DemoParticles01.

Both bodies have the same rectangular-bounds constraint to make them collide
with the window borders and are affected by the same mouse-controlled gravity.

A helper function to draw the outline of a blob is provided.

![DemoBlobAndParticles01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoBlobAndParticles01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoBlobAndParticles01.kt)

### DemoCircleConstraint01_kt



![DemoCircleConstraint01_ktKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoCircleConstraint01_ktKt.webp)

[source code](src/jvmDemo/kotlin/DemoCircleConstraint01_kt.kt)

### DemoContourConstraint01

Demonstration of the [NodeContourConstraint](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-node-contour-constraint/index.html).

A random hobby-contour is created and used for this particle simulation.
The gravity is not interactive but rotates clockwise over time.
The particles then "fall" onto part of the contour depending on the direction of gravity.

Try lowering the compliance of the `nodeContourConstraint` to make the particles follow the contour more strictly,
or increase the value for more looseness.

![DemoContourConstraint01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoContourConstraint01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoContourConstraint01.kt)

### DemoDifferentialBlob01

Demonstrates the use of repulsive forces optimized via bounding volume hierarchy
[BVH](https://orx.openrndr.org/orx-force-2d/org.openrndr.extra.force2d/-node-repulse-force-b-v-h/index.html).
The simulation begins with 4 regular stars placed on a 2x2 grid. The contours of the
shapes are quickly transformed by the repulsive forces, ensuring the vertices keep
a distance from each other.

The `searchRadius` for each shape's force is modulated over time using the cosine of time,
but out of sync, so when one radius increases, the other goes down, producing a mesmerizing
animation of the four shapes competing for space.

The shapes are rendered with `MULTIPLY` blend mode and unique hues
that together produce unique colors on the overlapping parts.

![DemoDifferentialBlob01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoDifferentialBlob01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoDifferentialBlob01.kt)

### DemoGraphLayout01

Demonstrates a custom simulation by manually creating nodes, links between those nodes,
and then a body containing them. The nodes are initially scattered around the drawer
bounds, and links are created for each node to the two nearest ones.

The simulation enforces a link-length constraint, a rectangular-bounds constraint for
the borders of the window, and a node-repulse force.

After a few seconds, the simulation falls into a semi-stable state, but this
is not a given and very much depends on the simulation values.

Not how instead of adding bodies one by one to the `ForceSimulation` they are passed
all at one in the constructor.

![DemoGraphLayout01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoGraphLayout01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoGraphLayout01.kt)

### DemoGraphLayout02

A variation of DemoGraphLayout01 that uses [ManyNodeForce] instead of
[NodeRepulseForce].

The simulation uses a high number of nodes connected to the
two nearest nodes, then tries to find a simple resting state.

`ManyNodeForce` uses a quadtree to deal with high node counts.

![DemoGraphLayout02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoGraphLayout02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoGraphLayout02.kt)

### DemoParticles01

A "hello world" of particle simulation. The particles are scattered on the drawer bounds
when the simulation starts.

Mouse-controllable gravity is the only active force, and two constraints are present: a
`rectangularBoundsConstraint` to keep the particles inside the program window and colliding
with its borders, and a `nodeCollisionConstraint` to make them collide with each other.

![DemoParticles01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-force-2d/images/DemoParticles01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoParticles01.kt)

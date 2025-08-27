# orx-turtle

Bezier (`ShapeContour`) backed [turtle graphics](https://en.wikipedia.org/wiki/Turtle_graphics).

## The turtle language

The basic turtle language consists of:

 * `rotate(degrees: Double)` to rotate
 * `forward(distance: Double` to walk forward
 * `penUp()` to raise the pen 
 * `penDown()` to lower the pen, this will start a new contour

Orientation/direction and position can be set directly
 * `position: Vector2` get/set position of the turtle, teleporting the turtle will start a new contour
 * `direction: Vector2` get/set direction of the turtle, setting direction will compute `orientation`
 * `orientation: Matrix44` the orientation matrix from which `direction` is evaluated
 * `isPenDown: Boolean` 
 
The language also holds some tools to manage the position and orientation of the turtle.

 * `resetOrientation()` will reset the orientation to the default orientation 
 * `push()` push the position and orientation on the stack
 * `pop()` pop the position and orientation from the stack
 * `pushOrientation()` push the orientation on the stack
 * `popOrientation()` pop the orientation on the stack
 * `pushPosition()` push the position on the stack
 * `popPosition()` pop the position from the stack

## The extended turtle language

 * `segment(s: Segment)` to draw a segment with its entrance tangent aligned to the turtle's orientation
 * `contour(c: ShapeContour)` to draw a contour with its entrance tangent aligned to the turtle's orientation
<!-- __demos__ -->
## Demos
### DemoTurtle01

/*
Drawing a square using the turtle interface.

![DemoTurtle01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-turtle/images/DemoTurtle01Kt.png)

[source code](src/jvmDemo/kotlin/DemoTurtle01.kt)

### DemoTurtle02

/*
A simple random walk made using the turtle interface.

![DemoTurtle02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-turtle/images/DemoTurtle02Kt.png)

[source code](src/jvmDemo/kotlin/DemoTurtle02.kt)

### DemoTurtle03

/*
Drawing shape contours aligned to the turtle's orientation.

![DemoTurtle03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-turtle/images/DemoTurtle03Kt.png)

[source code](src/jvmDemo/kotlin/DemoTurtle03.kt)

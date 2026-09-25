# orx-camera

2D and 3D cameras controllable via mouse and keyboard.

## Camera2D

To enable a 2D camera, add `extend(Camera2D())` to your program:

```kotlin
import org.openrndr.application
import org.openrndr.extra.camera.Camera2D

fun main() = application {
    program {
        extend(Camera2D())
        extend {
            drawer.circle(drawer.bounds.center, 300.0)
        }
    }
}
```

### Mouse controls 

* left-click drag - panning
* right-click drag - rotating
* middle-click - reset camera
* mouse-wheel - zoom

## Camera2DManual

A version of Camera2D that allows controlling
which elements are affected by the camera and which ones are not.

```kotlin
import org.openrndr.application
import org.openrndr.extra.camera.Camera2DManual

fun main() = application {
    program {
        val camera = Camera2DManual()
        extend {
            camera.isolated {
                // drawing in this block is affected by the camera
                drawer.rectangle(0.0, 0.0, 200.0, 100.0)
            }

            // static elements
            drawer.circle(drawer.bounds.center, 200.0)
        }
    }
}
```

You can have multiple layers, in any order, some static and others controlled by the camera.
Note that we don't `extend()` the camera in this case. 

## Orbital 3D camera

A 3D camera is often used to explore scenes with 3D meshes. To enable it, add `extend(Orbital())` to your program.

```kotlin
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.boxMesh
import org.openrndr.extra.meshgenerators.sphereMesh

fun main() = application {
    program {
        val sphere = sphereMesh(radius = 25.0)
        val cube = boxMesh(20.0, 20.0, 5.0, 5, 5, 2)

        extend(Orbital())

        extend {
            drawer.vertexBuffer(sphere, DrawPrimitive.LINE_LOOP)
            drawer.vertexBuffer(cube, DrawPrimitive.LINE_LOOP)
        }
    }
}
```

### Mouse controls

* left-click drag - rotate
* right-click drag - pan
* mouse-wheel - zoom

### Key bindings

* `w` - move forwards (+z)
* `s` - move backwards (-z)
* `Left` or `a` - strafe left (-x)
* `Right` or `d` - strafe right (+x)
* `Up` or `e`  -  move up (+y)
* `Down` or `q` -  move up (-y)
* `Page Up` -  zoom in
* `Page Down` -  zoom out

## OrbitalManual 3D camera

This is the equivalent to `Camera2DManual` but for 3D scenes. 
It lets you control what is affected by the camera and what is not.
A common use is to display 2D graphics and text around camera-controlled 3D models.

## ParametricOrbital 3D camera

This camera can only be controlled via code or a GUI, not with the mouse or keyboard.

Study the following demos for more examples.

<!-- __demos__ -->
## Demos
### DemoCamera2D01

#### Camera2D demo

click and drag the mouse for panning, use the mouse wheel for zooming

![DemoCamera2D01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoCamera2D01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoCamera2D01.kt)

### DemoCamera2D02

#### Camera2D demo with static elements

An approach for having certain elements not affected by the camera.
See DemoCamera2DManual01.kt for a new and simpler approach

![DemoCamera2D02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoCamera2D02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoCamera2D02.kt)

### DemoCamera2DManual01

Demonstrates how to use `Camera2DManual` to have
some elements affected by an interactive 2D camera combined with
other elements not affected by it.

In this example both PINK circles can be dragged, scaled and rotated
while the white circle in the middle is static.

![DemoCamera2DManual01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoCamera2DManual01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoCamera2DManual01.kt)

### DemoCamera2DManual02

Demonstrate the use of `Camera2DManual` to independently translate, scale and rotate one contour
in a collection.

When the mouse is clicked, the active contour is transformed using the camera view matrix,
then the camera is reset to its default state and whatever shape is under the mouse becomes
the new active contour.

As the mouse is dragged or its wheel scrolled, the camera is updated, affecting
how the active contour is rendered.

![DemoCamera2DManual02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoCamera2DManual02Kt.webp)

[source code](src/jvmDemo/kotlin/DemoCamera2DManual02.kt)

### DemoOrbital01

Demonstrate the use of `Orbital`, an interactive 3D camera
that can be controlled with a mouse and a keyboard.

![DemoOrbital01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoOrbital01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoOrbital01.kt)

### DemoOrbitalCamera01

Demonstrate the use of `OrbitalCamera`, `OrbitalControls`, `AxisHelper` and `GridHelper`.

Press the `t` key to toggle camera interaction, or `r` to reset the camera to its defaults.

![DemoOrbitalCamera01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoOrbitalCamera01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoOrbitalCamera01.kt)

### DemoOrbitalManual01

Demonstrate the use of an orbital camera to render a sphere and a cube in 3D space as wireframe meshes, positioned
and rendered independently using the camera's isolated drawing state. A stationary pink circle is also drawn in the
center of the scene.

Functionality:
- Initializes a sphere mesh and a cube mesh with predefined dimensions.
- Spawns an orbital camera, initially positioned away from the origin, to allow for focused rendering.
- Renders 3D wireframe shapes (sphere and cube) using the camera's isolated perspective.
- Draws a static 2D pink circle overlay at the window center.

![DemoOrbitalManual01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoOrbitalManual01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoOrbitalManual01.kt)

### DemoParametricOrbital01

Demonstrates the use of a `ParametricOrbital` camera.
This 3D camera can't be directly interacted with a mouse or a keyboard,
but only via a GUI (or via code).

The GUI state is saved when closing the program and loaded
when running it again.

The GUI also allows randomizing, loading and saving
its state to a file via the top buttons it displays.

![DemoParametricOrbital01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-camera/images/DemoParametricOrbital01Kt.webp)

[source code](src/jvmDemo/kotlin/DemoParametricOrbital01.kt)

# orx-camera

3D camera and controls for OPENRNDR. This supersedes the to be deprecated functionality in OPENRNDR.

## Usage

```kotlin
val camera = OrbitalCamera(Vector3.UNIT_Z * 1000.0, Vector3.ZERO, 90.0, 0.1, 2000.0)
val controls = OrbitalControls(camera, keySpeed = 10.0)

val debug3d = Debug3D(1000, 100)

extend(camera)
extend(controls) // adds both mouse and keyboard bindings
extend {
    debug3d.draw(drawer)

    drawer.perspective(90.0, width*1.0 / height, 0.1, 5000.0)
    drawer.shadeStyle = shadeStyle {
        vertexTransform = """x_viewMatrix = p_view"""
        parameter("view", camera.viewMatrix())
    }

    drawer.fill = ColorRGBa.PINK
    drawer.circle(0.0, 0.0, 500.0)
}
```

### Keybindings

* `w` - move forwards (+z)
* `s` - move backwards (-z)
* `Left` or `a` - strafe left (-x)
* `Right` or `d` - strafe right (+x)
* `Up` or `e`  -  move up (+y)
* `Down` or `q` -  move up (-y)
* `Page Up` -  zoom in
* `Page Down` -  zoom out

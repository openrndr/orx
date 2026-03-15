# orx-dnk3

A scene graph based 3d renderer with support for Gltf based assets

Status: in development

Supported Gltf features
- [x] Scene hierarchy
- [x] Loading mesh data
- [x] Glb
- [ ] Materials
  - [x] Basic materials
  - [x] Normal maps
  - [x] Metallic/roughness maps
  - [x] Skinning
  - [x] Double-sided materials
  - [ ] Transparency
- [x] Animations 
- [ ] Cameras
- [ ] Lights
<!-- __demos__ -->
## Demos
### DemoAnimations01

Demonstrates how to load and play an animated .glb file.

The `applyToTargets()` method expects an argument with a time in seconds.
The 0.6 offset is used just to get a more interesting screenshot to include in the README.md file in GitHub.

![DemoAnimations01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoAnimations01Kt.png)

[source code](src/demo/kotlin/DemoAnimations01.kt)

### DemoCamera01

Demonstrates how the view and projection matrices used for rendering in OPENRNDR can
be controlled using an animated camera found in a loaded .glb file.

![DemoCamera01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoCamera01Kt.png)

[source code](src/demo/kotlin/DemoCamera01.kt)

### DemoIrrProbe01

Advanced lighting demonstration featuring irradiance probes, a dynamic node animated via code,
and an interactive Orbital camera.

![DemoIrrProbe01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoIrrProbe01Kt.png)

[source code](src/demo/kotlin/DemoIrrProbe01.kt)

### DemoLights01

Demonstrates how to load a .glb file containing an animated scene.
The scene contains a cube and an animated point-light.

The scene contains a list of animations, which need to be updated using the `.applyToTargets()` method,
otherwise the time in the animation is still. The method expects a time in seconds. In this demo,
we pass a time that loops based on the duration of the animation. Note that it would be easy to
pass a different time, slower or faster than real time, play it backwards or even travel back and forth
in time.

An interactive orbital camera is enabled, letting you use the mouse to control the camera position,
direction, and zoom.

![DemoLights01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoLights01Kt.png)

[source code](src/demo/kotlin/DemoLights01.kt)

### DemoLights02

Demonstrates how to load a .glb file containing an animated scene.
The scene contains a floor, a cube, and an animated light.
When rendered, the light casts the shadow of the cube onto the floor.

The scene contains a list of animations, which need to be updated using the `.applyToTargets()` method,
otherwise the time in the animation is still. The method expects a time in seconds. In this demo,
we pass a time that loops based on the duration of the animation. Note that it would be easy to
pass a different time, slower or faster than real time, play it backwards or even travel back and forth
in time.

An interactive orbital camera is enabled, letting you use the mouse to control the camera position,
direction, and zoom.

![DemoLights02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoLights02Kt.png)

[source code](src/demo/kotlin/DemoLights02.kt)

### DemoLights03

Demonstrates how to load a .glb file containing a scene with a floor, a cube, a sphere and a directional light.
The light hits the cube, which casts a shadow onto the sphere and the floor.

An interactive orbital camera is enabled, letting you use the mouse to control the camera position,
direction, and zoom.

![DemoLights03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoLights03Kt.png)

[source code](src/demo/kotlin/DemoLights03.kt)

### DemoObject01

Demonstrates how to collect the 3D meshes found in a gltf file and render them
in the program window.

The default settings of an Orbital camera would render the meshes too close,
therefore the `far`, `lookAt`, `eye` and `fov` properties are adjusted to provide
a better view of the models.

Meshes can provide (or not) an `indexBuffer`. The program how to render both types of mesh.

![DemoObject01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoObject01Kt.png)

[source code](src/demo/kotlin/DemoObject01.kt)

### DemoScene01

Demonstrates how to create a 3D Scene and add children to it:
one child containing two lights, and another containing a loaded 3D model.
The model contains two textures: one is the base color and another is its metallic roughness.

![DemoScene01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoScene01Kt.png)

[source code](src/demo/kotlin/DemoScene01.kt)

### DemoScene02

Demonstrates how to create a 3D Scene and add children to it:
one child containing two lights, and another containing a loaded 3D model of a rubber duck.
The model contains two textures: one is the base color and another is its metallic roughness.

![DemoScene02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoScene02Kt.png)

[source code](src/demo/kotlin/DemoScene02.kt)

### DemoScene03

Demonstrates how to create a 3D Scene and add children to it:
one child contains lights, another contains a spherical 3D mesh
generated via code. A PBR (Physically Based Rendering) material
is created, configured, and applied to the mesh.

![DemoScene03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoScene03Kt.png)

[source code](src/demo/kotlin/DemoScene03.kt)

### DemoSegmentContours01

Demonstrate the use of a custom renderer, in this case a `segmentContourRenderer`,
which renders the outline of a 3D object, an animated 3D fox loaded  from a .glb file.

![DemoSegmentContours01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoSegmentContours01Kt.png)

[source code](src/demo/kotlin/DemoSegmentContours01.kt)

### DemoSkinning01

Demonstrate the use of the dry renderer
to render an animated 3D fox loaded
from a .glb file.

Note that the file contains 3 animations.
Try animations 0, 1, and 2.

![DemoSkinning01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-jvm/orx-dnk3/images/DemoSkinning01Kt.png)

[source code](src/demo/kotlin/DemoSkinning01.kt)

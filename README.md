# ORX (OPENRNDR EXTRA)

 [ ![Download](https://api.bintray.com/packages/openrndr/openrndr/orx/images/download.svg) ](https://bintray.com/openrndr/openrndr/orx/_latestVersion)

A growing library of assorted data structures, algorithms and utilities.

<!-- __orxListBegin__ -->
| name&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | description |
| --- | --- |
| [`orx-boofcv`](orx-boofcv/README.md) | Helper functions to ease working with the BoofCV computer vision library and its data types. |
| [`orx-camera`](orx-camera/README.md) | 3D camera controllable via mouse and keyboard. |
| [`orx-compositor`](orx-compositor/README.md) | Toolkit to make composite (layered) images using blend modes and filters. |
| [`orx-easing`](orx-easing/README.md) | Provides easing functions for smooth animation or non-linear interpolation. |
| [`orx-file-watcher`](orx-file-watcher/README.md) | Monitor files on disk and auto-reload them if they change. |
| [`orx-filter-extension`](orx-filter-extension/README.md) | To apply graphics filters on every animation frame using `extend(FILTER_NAME())`. |
| [`orx-fx`](orx-fx/README.md) | Ready-to-use visual effects or filters. Most include [orx-parameters](https://github.com/openrndr/orx/tree/master/orx-parameters) annotations  so they can be easily controlled via orx-gui. |
| [`orx-glslify`](orx-glslify/README.md) | Load glslify compatible shaders from [npm](https://www.npmjs.com/search?q=glslify). |
| [`orx-gradient-descent`](orx-gradient-descent/README.md) | Finds equation inputs that output a minimum value: easy to use gradient descent based minimizer. |
| [`orx-gui`](orx-gui/README.md) | Automatic UI (sliders, buttons, etc.) generated from annotated classes and properties. Uses `orx-panel` and `orx-parameters`. |
| [`orx-image-fit`](orx-image-fit/README.md) | Draws the given image making sure it fits (`contain`) or it covers (`cover`) the specified area. |
| [`orx-integral-image`](orx-integral-image/README.md) | CPU and GPU-based implementation for integral images (summed area tables) |
| [`orx-interval-tree`](orx-interval-tree/README.md) | For querying a data set containing time segments (start time and end time) when we need all entries containing a specific time value. Useful when creating a timeline. |
| [`orx-jumpflood`](orx-jumpflood/README.md) | Takes an image and calculates either a distance field or a direction field.  GPU accelerated, 2D. Results are provided as an image. |
| [`orx-kdtree`](orx-kdtree/README.md) | Fast search of the point closest to the queried point in a data set. 2D, 3D and 4D. |
| [`orx-keyframer`](orx-keyframer/README.md) | Create animated timelines by specifying properties and times in keyframes,  then play it back at any speed (even backwards) automatically interpolating properties.  Save, load, use mathematical expressions and callbacks. Powerful and highly reusable. |
| [`orx-kinect-v1`](orx-kinect-v1/README.md) | Support for the Kinect V1 RGB+Depth camera. |
| [`orx-mesh-generators`](orx-mesh-generators/README.md) | Generates 3D meshes: sphere, box, cylinder, plane, dodecahedron. |
| [`orx-midi`](orx-midi/README.md) | Basic MIDI support for keyboards and controllers. Send and receive note and control change events. |
| [`orx-no-clear`](orx-no-clear/README.md) | Provides the classical draw-without-clearing-the-screen functionality that OPENRNDR does not provide by default. |
| [`orx-noise`](orx-noise/README.md) | Randomness for every type of person: Perlin, uniform, value, simplex, fractal and many other types of noise. |
| [`orx-obj-loader`](orx-obj-loader/README.md) | Simple loader for Wavefront .obj 3D mesh files. |
| [`orx-olive`](orx-olive/README.md) | Provides live coding functionality: updates a running OPENRNDR program when you save your changes. |
| [`orx-osc`](orx-osc/README.md) | Open Sound Control makes it possible to send and receive messages from other OSC enabled programs in the same or a different computer. Used to create multi-application or multi-device software. |
| [`orx-palette`](orx-palette/README.md) | Provides 300 color palettes gathered from different sources, organized in 3 collections. |
| [`orx-panel`](orx-panel/README.md) | The OPENRNDR UI toolkit. Provides buttons, sliders, text, a color picker and much more. HTML/CSS-like. |
| [`orx-parameters`](orx-parameters/README.md) | Provides annotations and tools for turning Kotlin properties into introspectable parameters. Used by [`orx-gui`](../orx-gui/README.md) to automatically generate user interfaces. |
| [`orx-poisson-fill`](orx-poisson-fill/README.md) | Post processing effect that fills transparent parts of the image interpolating the edge pixel colors. GPU-based. |
| [`orx-rabbit-control`](orx-rabbit-control/README.md) | Automatically creates a remote UI to control your OPENRNDR program from a mobile device or a different computer. Alternative to `orx-gui`. |
| [`orx-runway`](orx-runway/README.md) | Interfaces with the RunwayML machine learning library that provides features like motion capture, image synthesis, object recognition, style transfer and more. More info at [runwayml.com](https://runwayml.com/). |
| [`orx-shade-styles`](orx-shade-styles/README.md) | Collection of shader based fills and strokes. Currently includes 4 types of gradient fills. |
| [`orx-shader-phrases`](orx-shader-phrases/README.md) | A library that provides a `#pragma import` statement for shaders by using the JVM class loader. |
| [`orx-shapes`](orx-shapes/README.md) | Collection of 2D shape generators (polygon, star, rounded rectangle) and shape modifiers. |
| [`orx-syphon`](orx-syphon/README.md) | Send frames to- and from OPENRNDR to other applications in real time using _Syphon_ for Mac. |
| [`orx-temporal-blur`](orx-temporal-blur/README.md) | Post-processing temporal-blur video effect. CPU intense, therefore not intended  for use with the `ScreenRecorder` extension or other real-time uses. |
| [`orx-time-operators`](orx-time-operators/README.md) | A collection of time-sensitive functions aimed at controlling raw data over-time,  such as Envelope and LFO. |
| [`orx-timer`](orx-timer/README.md) | Simple timer functionality providing `repeat`, to run code with a given interval and `timeOut`, to run code once after a given delay. |
<!-- __orxListEnd__ -->

# Developer notes

## Create and use local builds of the library

run `./gradlew publishToMavenLocal -Prelease.version=0.4.0-SNAPSHOT` (or import in IntelliJ IDEA and edit the run configuration)

In an [`openrndr-template`](https://github.com/openrndr/openrndr-template) based project set `orxUseSnapshot = true` in order to use the snapshot build.
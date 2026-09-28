# ORX (OPENRNDR EXTRA)

[![ORX maven badge](https://img.shields.io/maven-central/v/org.openrndr.extra/orx-noise-jvm?style=flat&color=%23FFC0CB
)](https://mvnrepository.com/artifact/org.openrndr.extra)

A growing library of assorted data structures, algorithms and utilities to
complement [OPENRNDR](https://github.com/openrndr/openrndr). 
Multiplatform, unless they deal with hardware or depend on binary libraries. Those are JVM-only.

Find an auto-generated API documentation page at https://orx.openrndr.org/.

## Usage

To make use of these extensions clone the [OPENRNDR template](https://github.com/openrndr/openrndr-template), uncomment the ones you need in its [build.gradle.kts](https://github.com/openrndr/openrndr-template/blob/master/build.gradle.kts) file, and reload Gradle. Cloning this repo is optional but useful to run the demos in each ORX folder, to study the source code, and to contribute to existing or new ORX'es.

<!-- __orxListBegin__ -->

## Multiplatform

| name&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | description |
| --- | --- |
| [`openrndr-demos`](openrndr-demos/) | OPENRNDR demos for testing functionality. |
| [`orx-bvh`](orx-bvh/) | Bounding Volume Hierarchy: a tree structure used to speed up ray/geometry intersection tests. |
| [`orx-camera`](orx-camera/) | 2D and 3D cameras controllable via mouse and keyboard. |
| [`orx-color`](orx-color/) | Color spaces, palettes, histograms, named colors. |
| [`orx-composition`](orx-composition/) | Create, inspect, and modify vector-based designs. |
| [`orx-compositor`](orx-compositor/) | Make composite (layered) images using blend modes and filters. |
| [`orx-delegate-magic`](orx-delegate-magic/) | Magical property delegators for tracking variable change or interpolate towards the value of a variable. |
| [`orx-easing`](orx-easing/) | Easing functions for smooth animation or non-linear interpolation. |
| [`orx-envelopes`](orx-envelopes/) | ADSR (Attack, Decay, Sustain, Release) envelopes and tools. |
| [`orx-expression-evaluator`](orx-expression-evaluator/) | Evaluate strings containing mathematical expressions. |
| [`orx-expression-evaluator-typed`](orx-expression-evaluator-typed/) | Evaluate strings containing *typed* mathematical expressions. |
| [`orx-fcurve`](orx-fcurve/) | 1-dimensional function curves constructed from 2D bezier functions. Often used to control a property over time.  `x` values don't have any units, but they often represent a duration in seconds. |
| [`orx-fft`](orx-fft/) | Simple forward and inverse Fast Fourier transform routine. |
| [`orx-force-2d`](orx-force-2d/) | 2D extended position-based dynamics (XPBD) for simulating physical forces. |
| [`orx-fx`](orx-fx/) | Ready-to-use GPU-based visual effects or filters. Most include [orx-parameters](https://github.com/openrndr/orx/tree/master/orx-parameters) annotations  so they can be easily controlled via orx-gui. |
| [`orx-g-code`](orx-g-code/) | G-code generator for CNC Machines such as pen plotters, laser engravers, 3D printers, and more. |
| [`orx-gradient-descent`](orx-gradient-descent/) | Finds equation inputs that output a minimum value: easy to use gradient descent based minimizer. |
| [`orx-hash-grid`](orx-hash-grid/) | 2D space partitioning for fast point queries. |
| [`orx-image-fit`](orx-image-fit/) | Draws an image ensuring it fits or covers the specified `Rectangle`. |
| [`orx-integral-image`](orx-integral-image/) | CPU and GPU-based implementation for integral images (summed area tables) |
| [`orx-interval-tree`](orx-interval-tree/) | Query a data set containing time segments (with start and end time) to get all entries containing a specific time value. Useful when creating a timeline. |
| [`orx-jumpflood`](orx-jumpflood/) | Calculates distance or direction fields from an image. GPU accelerated, 2D. Results are provided as an image. |
| [`orx-kdtree`](orx-kdtree/) | Fast search of points closest to the queried point in a data set. 2D, 3D and 4D. |
| [`orx-marching-squares`](orx-marching-squares/) | Extract contours from mathematical functions. |
| [`orx-math`](orx-math/) | Mathematical utilities, including complex numbers, linear ranges, simplex ranges, matrices and radial basis functions (RBF). |
| [`orx-mesh`](orx-mesh/) | Data structures and utilities for representing, manipulating,  and transforming 3D polygon meshes. |
| [`orx-mesh-generators`](orx-mesh-generators/) | 3D-mesh generating functions and DSL. |
| [`orx-mesh-noise`](orx-mesh-noise/) | Generate random samples on the surface of a mesh. |
| [`orx-module-catalog`](orx-module-catalog/) | Gradle version catalog with ORX dependency definitions  and bundles for `openrndr-template`. |
| [`orx-no-clear`](orx-no-clear/) | Classical "draw-without-clearing-the-screen" functionality. |
| [`orx-noise`](orx-noise/) | Randomness for every type of person: Perlin, uniform, value, simplex, fractal and many other types of noise. |
| [`orx-obj-loader`](orx-obj-loader/) | OBJ reader and writer library (Wavefront 3D mesh files). |
| [`orx-palette`](orx-palette/) | Collections of color palettes and tools for interacting with them. |
| [`orx-parameters`](orx-parameters/) | Annotations for turning Kotlin properties into introspectable parameters. Used by [`orx-gui`](../orx-jvm/orx-gui/README.md) to generate user interfaces, and  by [`orx-midi`](../orx-jvm/orx-midi/README.md) to bind hardware inputs to variables. |
| [`orx-property-watchers`](orx-property-watchers/) | Tools for setting up property watcher based pipelines |
| [`orx-quadtree`](orx-quadtree/) | A [Quadtree](https://en.wikipedia.org/wiki/Quadtree) is a spatial partioning tree structure meant to provide fast spatial queries such as nearest points within a range. |
| [`orx-shade-styles`](orx-shade-styles/) | Shader based fills and strokes, including various types of gradient fills. |
| [`orx-shader-phrases`](orx-shader-phrases/) | Provides a `#pragma import` statement for shaders. |
| [`orx-shapes`](orx-shapes/) | 2D and 3D shape generators and modifiers. |
| [`orx-svg`](orx-svg/) | SVG reader and writer library. |
| [`orx-temporal-blur`](orx-temporal-blur/) | Post-processing temporal-blur video effect. CPU intense, not intended for real-time uses. |
| [`orx-text-on-contour`](orx-text-on-contour/) | Write texts on contours. |
| [`orx-text-writer`](orx-text-writer/) | Write texts with layouts. |
| [`orx-time-operators`](orx-time-operators/) | Time-sensitive functions for controlling raw data over-time,  such as Envelope and LFO. |
| [`orx-timer`](orx-timer/) | Timer functionality providing `repeat`, to run code with a given interval and `timeOut`, to run code once after a given delay. |
| [`orx-triangulation`](orx-triangulation/) | **Delaunay** triangulation and **Voronoi** diagrams. |
| [`orx-turtle`](orx-turtle/) | Bezier (`ShapeContour`) backed [turtle graphics](https://en.wikipedia.org/wiki/Turtle_graphics). |
| [`orx-view-box`](orx-view-box/) | Create independent views inside one program window. |

## JVM only

| name&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; | description |
| --- | --- |
| [`orx-axidraw`](orx-jvm/orx-axidraw/) | GUI for configuring and plotting with an Axidraw pen-plotter. |
| [`orx-boofcv`](orx-jvm/orx-boofcv/) | Helper functions to ease working with the BoofCV computer vision library and its data types. |
| [`orx-chataigne`](orx-jvm/orx-chataigne/) | Expose variables to [Chataigne](http://benjamin.kuperberg.fr/chataigne/en) and any other applications that can interface with it. The current implementation makes use of the OSC protocol and supports `Double` and `ColorRGBa`. |
| [`orx-depth-camera-calibrator`](orx-jvm/orx-depth-camera-calibrator/) | Depth camera calibration helper. |
| [`orx-dnk3`](orx-jvm/orx-dnk3/) | A scene-graph based 3D renderer with support for Gltf based assets |
| [`orx-file-watcher`](orx-jvm/orx-file-watcher/) | Monitor files on disk and auto-reload them if they change. |
| [`orx-git-archiver`](orx-jvm/orx-git-archiver/) | An extension that hooks into `Program.requestAssets` to commit changed code to Git and provide filenames based on the commit hash. |
| [`orx-git-archiver-gradle`](orx-jvm/orx-git-archiver-gradle/) | A Gradle plugin that turns a git history and a `screenshots` directory into a Markdown file. Works in combination with [orx-git-archiver](../orx-git-archiver). |
| [`orx-gui`](orx-jvm/orx-gui/) | Automatic UI (sliders, buttons, etc.) generated from annotated classes and properties. Uses `orx-panel` and `orx-parameters`. |
| [`orx-keyframer`](orx-jvm/orx-keyframer/) | Create animated timelines by specifying properties and times in keyframes, then play it back at any speed (even backwards) automatically interpolating properties. Save, load, use mathematical expressions and callbacks. Powerful and highly reusable. |
| [`orx-kinect-v1`](orx-jvm/orx-kinect-v1/) | Kinect V1 RGB and depth camera support. |
| [`orx-kotlin-parser`](orx-jvm/orx-kotlin-parser/) | Parse .kt (Kotlin) files and process the nodes found in their source code. |
| [`orx-midi`](orx-jvm/orx-midi/) | MIDI support for keyboards and controllers. Send and receive note and control change events. Bind inputs to variables. |
| [`orx-minim`](orx-jvm/orx-minim/) | Sound synthesis and analysis via the [Minim](https://code.compartmental.net/tools/minim/) sound library. |
| [`orx-olive`](orx-jvm/orx-olive/) | Live coding functionality: updates a running OPENRNDR program when you save your changes. |
| [`orx-osc`](orx-jvm/orx-osc/) | Use Open Sound Control to send and receive messages from other OSC enabled programs in the same or a different computer. Used to create multi-application or multi-device software. |
| [`orx-panel`](orx-jvm/orx-panel/) | The OPENRNDR UI toolkit. Provides buttons, sliders, text, a color picker and much more. HTML/CSS-like. |
| [`orx-poisson-fill`](orx-jvm/orx-poisson-fill/) | Post processing effect that fills transparent parts of the image interpolating the edge pixel colors. GPU-based. |
| [`orx-processing`](orx-jvm/orx-processing/) | Seamless conversion between [Processing](https://processing.org) and OPENRNDR types to bridge the gap between the two frameworks. |
| [`orx-rabbit-control`](orx-jvm/orx-rabbit-control/) | Creates a web-based remote UI to control your OPENRNDR program from a mobile device or a different computer. Alternative to `orx-gui`. |
| [`orx-remote-control`](orx-jvm/orx-remote-control/) | Add remote control functionality to Programs via HTTP API. |
| [`orx-syphon`](orx-jvm/orx-syphon/) | Send frames to- and from OPENRNDR to other applications in real time using _Syphon_ for Mac. |
| [`orx-video-profiles`](orx-jvm/orx-video-profiles/) | GIF, H265, PNG, Prores, TIFF and Webp `VideoWriterProfile`s for `ScreenRecorder` and `VideoWriter`. |
<!-- __orxListEnd__ -->

# Developer notes

## Publish and use local builds of the library in your applications

First, build and publish [OPENRNDR](https://github.com/openrndr/openrndr) to the local maven repository:

Run (or import in IntelliJ IDEA and edit the run configuration).
```sh
# In openrndr repository
./gradlew publishToMavenLocal snapshot
``` 

This command will build and publish a snapshot of the next version of the library to your local maven repository.
The exact version will be shown in the console output during the build process.

Now you can run the same command again but for this repository. 

```sh
# In orx repository
./gradlew publishToMavenLocal snapshot
``` 

It will automatically use the locally published snapshot of OPENRNDR for building ORX and will publish ORX to your local
maven repository with the same logic as before.

Once that's done, you can use the local builds of OPENRNDR and ORX in
your [openrndr-template](https://github.com/openrndr/openrndr-template) by specifying the version that was published.

Take a look at the [wiki](https://github.com/openrndr/openrndr/wiki/Building-OPENRNDR-and-ORX) for a more detailed walk-through.
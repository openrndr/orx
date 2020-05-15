# ORX (OPENRNDR EXTRA)

 [ ![Download](https://api.bintray.com/packages/openrndr/openrndr/orx/images/download.svg) ](https://bintray.com/openrndr/openrndr/orx/_latestVersion)

A growing library of assorted data structures, algorithms and utilities.
- [`orx-camera`](orx-camera/README.md), 3d camera and controls
- [`orx-compositor`](orx-compositor/README.md), a simple toolkit to make composite (layered) images
- [`orx-easing`](orx-easing/README.md), a collection of easing functions.
- [`orx-file-watcher`](orx-file-watcher/README.md), `Program` extension method that allows monitoring and hot loading from files.
- [`orx-filter-extension`](orx-filter-extension/README.md), `Program` extension method that provides Filter based `extend()`
- [`orx-glslify`](orx-glslify/README.md), use [glslify](http://stack.gl/packages/) modules within openrndr shaders
- [`orx-gui`](orx-gui/README.md), automatic UI generation for annotated classes and properties
- [`orx-gradient-descent`](orx-gradient-descent/README.md), a gradient descent based minimizer
- [`orx-image-fit`](orx-image-fit/README.md), easier drawing of images 
- [`orx-integral-image`](orx-integral-image/README.md), CPU-based and GPU-based implementation for integral images (summed area tables)
- [`orx-interval-tree`](orx-interval-tree/README.md), data structure for accelerating point-in-interval queries.
- [`orx-jumpflood`](orx-jumpflood/README.md), a filter/shader based implementation of the jump flood algorithm for finding fast approximate (directional) distance fields
- `orx-kdtree`, a kd-tree implementation for fast nearest point searches
- [`orx-keyframer`](orx-keyframer/README.md), versatile parametric keyframer
- [`orx-kinect-v1`](orx-kinect-v1/README.md), utilities to use Kinect V1 RGB-D sensors in OPENRNDR programs. 
- [`orx-mesh-generators`](orx-mesh-generators/README.md), triangular mesh generators
- [`orx-midi`](orx-midi/README.md), midi controller interface
- [`orx-noise`](orx-noise/README.md), library for random number generation and noise
- [`orx-no-clear`](orx-no-clear/README.md), a simple extension that provides drawing without clearing the background
- [`orx-obj-loader`](orx-obj-loader/README.md), simple Wavefront .obj mesh loader
- [`orx-olive`](orx-olive/README.md), extensions that turns OPENRNDR in to a live coding environment
- [`orx-osc`](orx-osc/README.md), open sound control interface 
- [`orx-palette`](orx-palette/README.md), manage color palettes 
- [`orx-panel`](orx-panel/README.md), the OPENRNDR ui library
- [`orx-parameters`](orx-parameters/README.md), property annotations that allow for automatic ui generation
- [`orx-poison-fill`](orx-poisson-fill/README.md), GPU implementation for Poisson fills.  
- [`orx-rabbit-control`](orx-rabbit-control/README.md), RabbitControl extension using `orx-parameters` 
- [`orx-runway`](orx-runway/README.md), support for RunwayML
- [`orx-shade-styles`](orx-shade-styles/README.md), a collection of shade style presets
- [`orx-shapes`](orx-shapes), tools for generating and modifying shapes
- [`orx-syphon`](orx-syphon/README.md), send frames to- and from OPENRNDR and other applications in real time using Syphon
- [`orx-temporal-blur`](orx-temporal-blur/README.md), temporal (motion) blur for video production.
- [`orx-time-operators`](orx-time-operators/README.md), A collection of time-sensitive functions aimed at controlling raw data over-time.
- [`orx-timer`](orx-timer/README.md), simple timer functionality for OPENRNDR

# Developer notes

## Create and use local builds of the library

run `./gradlew publishToMavenLocal -Prelease.version=0.4.0-SNAPSHOT` (or import in IntelliJ IDEA and edit the run configuration)

In an [`openrndr-template`](https://github.com/openrndr/openrndr-template) based project set `orxUseSnapshot = true` in order to use the snapshot build.

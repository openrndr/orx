# ORX (OPENRNDR EXTRA)

[ ![Download](https://api.bintray.com/packages/openrndr/openrndr/orx/images/download.svg?version=0.3.35) ](https://bintray.com/openrndr/openrndr/orx/0.3.35/link)

A growing library of assorted data structures, algorithms and utilities.

- [`orx-camera`](orx-camera/README.md), 3d camera and controls
- [`orx-compositor`](orx-compositor/README.md), a simple toolkit to make composite (layered) images
- [`orx-easing`](orx-easing/README.md), a collection of easing functions.
- [`orx-gradient-descent`](orx-gradient-descent/README.md), a gradient descent based minimizer
- [`orx-file-watcher`](orx-file-watcher/README.md), `Program` extension method that allows monitoring and hot loading from files.
- [`orx-filter-extension`](orx-filter-extension/README.md), `Program` extension method that provides Filter based `extend()`
- [`orx-integral-image`](orx-integral-image/README.md), CPU-based and GPU-based implementation for integral images (summed area tables)
- [`orx-interval-tree`](orx-interval-tree/README.md), data structure for accelerating point-in-interval queries.
- `orx-jumpflood`, a filter/shader based implementation of the jump flood algorithm for finding fast approximate (directional) distance fields
- `orx-kdtree`, a kd-tree implementation for fast nearest point searches
- [`orx-kinect-v1`](orx-kinect-v1/README.md), utilities to use Kinect V1 RGB-D sensors in OPENRNDR programs. 
- [`orx-mesh-generators`](orx-mesh-generators/README.md), triangular mesh generators
- [`orx-midi`](orx-midi/README.md), midi controller interface
- [`orx-noise`](orx-noise/README.md), library for random number generation and noise
- [`orx-no-clear`](orx-no-clear/README.md), a simple extension that provides drawing without clearing the background
- [`orx-obj-loader`](orx-obj-loader/README.md), simple Wavefront .obj mesh loader
- [`orx-olive`](orx-olive/README.md), extensions that turns OPENRNDR in to a live coding environment
- [`orx-osc`](orx-osc/README.md), open sound control interface 
- [`orx-palette`](orx-palette/README.md), manage color palettes 
- [`orx-temporal-blur`](orx-temporal-blur/README.md), temporal (motion) blur for video production.
# Developer notes

## Create and use local builds of the library

run `./gradlew publishToLocalMaven -Prelease.version=0.4.0-SNAPSHOT` (or import in IntelliJ IDEA and edit the run configuration)

In an [`openrndr-template`](https://github.com/openrndr/openrndr-template) based project set `orxUseSnapshot = true` in order to use the snapshot build.

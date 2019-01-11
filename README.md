# ORX (OPENRNDR EXTRA)

[![](https://jitpack.io/v/openrndr/orx.svg)](https://jitpack.io/#openrndr/orx)

A growing library of assorted data structures, algorithms and utilities.

- [`orx-camera`](orx-camera/README.md), 3d camera and controls
- [`orx-compositor`](orx-compositor/README.md), a simple toolkit to make composite (layered) images
- [`orx-filter-extension`](orx-filter-extension/README.md), Program extension method that provides Filter based `extend()`
- [`orx-integral-image`](orx-integral-image/README.md), a CPU-based implementation for integral images (summed area tables)
- `orx-jumpflood`, a filter/shader based implementation of the jump flood algorithm for finding fast approximate (directional) distance fields
- `orx-kdtree`, a kd-tree implementation for fast nearest point searches
- [`orx-mesh-generators`](orx-mesh-generators/README.md), triangular mesh generators
- [`orx-noise`](orx-noise/README.md), library for random number generation and noise
- [`orx-no-clear`](orx-no-clear/README.md), a simple extension that provides drawing without clearing the background
- [`orx-obj-loader`](orx-obj-loader/README.md), simple Wavefront .obj mesh loader

## Usage
ORX 0.0.18 is built against OPENRNDR 0.3.32, make sure you use this version in your project. Because OPENRNDR's API is pre 1.0 it tends to change from time to time.

The easiest way to add ORX to your project is through the use of Jitpack. [Jitpack](http://jitpack.io) is a service that pulls Gradle based libraries from Github, builds them and serves the jar files.

To setup Jitpack support in your project all you have to do is add the Jitpack repository to your `repositories {}`. It is advised to have the jitpack repository as the last entry.
```
repositories {
    maven { url 'https://jitpack.io' }
}
```

You can then add any of the ORX artefacts to your `dependencies {}`:
```
dependencies {
    compile 'com.github.openrndr.orx:<orx-artifact>:v0.0.18'
}
```

For example if you want to use the `orx-no-clear` artifact one would use:
```
dependencies {
    compile 'com.github.openrndr.orx:orx-no-clear:v0.0.18'
}
```

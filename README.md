# ORX (OPENRNDR EXTRA)

A growing library of assorted data structures, algorithms and utilities.

- [`orx-integral-image`](orx-integral-image/README.md), a CPU-based implementation for integral images (summed area tables)
- `orx-jumpflood`, a filter/shader based implementation of the jump flood algorithm for finding fast approximate (directional) distance fields
- `orx-kdtree`, a kd-tree implementation for fast nearest point searches
- [`orx-no-clear`](orx-no-clear/README.md), a simple extension that provides drawing without clearing the background

## Usage
ORX is built against OPENRNDR 0.3.28, make sure you use this version in your project. Because OPENRNDR's API is pre 1.0 it tends to change from time to time.

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
    compile 'com.github.openrndr.orx:<orx-artifact>:v0.0.9'
}
```

For example if you want to use the `orx-no-clear` artifact one would use:
```
dependencies {
    compile 'com.github.openrndr.orx:orx-no-clear:v0.0.9'
}
```

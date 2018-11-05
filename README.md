# ORX (OPENRNDR EXTRA)

A growing library of assorted data structures, algorithms and utilities.

- orx-kdtree, a kd-tree implementation for fast nearest point searches
- orx-jumpflood, a filter/shader based implementation of the jump flood algorithm for finding fast approximate (directional) distance fields
- orx-integral-image, a CPU-based implementation for integral images (summed area tables)

## Usage

ORX is build against OPENRNDR 0.3.28, make sure you use this version in your project.

Easiest way to add ORX to your project is through the use of Jitpack

Add repository:
```
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add dependency:
```
dependencies {
    compile 'com.github.openrndr.orx:<orx-artifact>:v0.0.8'
}
```

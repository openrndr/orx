# Contributing to ORX

Thank you for your interest in contributing to ORX :-)

This repository contains the OPENRNDR extras: a growing library of assorted data structures, algorithms and utilities to complement OPENRNDR.
Other repositories you can contribute to are the [core OPENRNDR](https://github.com/openrndr/openrndr/), 
the [guide](https://github.com/openrndr/openrndr-guide/) and the [template](https://github.com/openrndr/openrndr-template/).

Please read the [general information about contributing to OPENRNDR](https://github.com/openrndr/openrndr/blob/master/CONTRIBUTING.md).
This document will focus on specific details about the ORX repository.

## Overview

There are two types of ORX extras: 
- JVM only. Subfolders of `/orx-jvm/`. These run only on Desktop (not in web browsers).
- Multiplatform. Other `/orx-.../` folders. These run both on Desktop and web browsers.

Each orx folder contains a `README.md`, a `build.gradle.kts` file and a `src` folder. 
Please explore several orx directories to get a feel for how they look like.

Various Gradle tasks take care of updating the README files. 

## README.md

Assuming you are creating an orx called `magic`, the readme file should contain the following:

```
# orx-magic

One or more lines including a short description to display on the root README.md.
One or more lines including a short description to display on the root README.md.
One or more lines including a short description to display on the root README.md.

[Main content describing the usage of orx-magic goes here]

<!-- __demos__ -->
```

1. Start with a markdown header with the name of the orx followed by an empty line.
2. One or more lines with a brief description to show on the root `README.md`, followed by an empty line.
   (The `buildMainReadme` Gradle task will extract this description and update the root `README.md`).
3. A detailed description (a guide) of how to use the orx, possibly with code examples in code fences like
   ````
   ```kotlin
     //code example
   ```
   ````
4. If the orx includes demos (more below), running the `CollectScreenShots` Gradle task will append `<!-- __demos__ -->`
   to the readme followed by a list of automatically generated screenshots of the demos and links to their source code.
   This is specially useful for orx'es that produce graphical output, but less so for orx'es that interface
   with hardware (like `orx-midi`).

## build.gradle.kts

ORX `build.gradle.kts` files declare their dependencies and most follow the same structure. 
Please explore various build files and find the simplest one that matches your use case. 
Note that the JVM ones are somewhat simpler than the multiplatform ones.

The `plugins` section includes either ``org.openrndr.extra.convention.`kotlin-multiplatform` `` or
``org.openrndr.extra.convention.`kotlin-jvm` `` depending on the orx type.

### JVM

The JVM build files declare separate dependencies for the orx itself (`implementation`) and for usage demos
(`demoImplementation`). 
See an [example](https://github.com/openrndr/orx/blob/master/orx-jvm/orx-dnk3/build.gradle.kts).

### Multiplatform

The multiplatform build files can have 4 blocks: `commonMain`, `commonTest`, `jvmTest` and `jvmDemo`. 
See an [example](https://github.com/openrndr/orx/blob/master/orx-color/build.gradle.kts).

## Source folder

To do.

## Demos

To do.

## Gradle tasks

To do.

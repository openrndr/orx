# Contributing to ORX

Thank you for your interest in contributing to ORX :-)

This repository contains the OPENRNDR extras: a growing library of assorted data structures, algorithms and utilities to complement OPENRNDR.
Other repositories you can contribute to are the [core OPENRNDR](https://github.com/openrndr/openrndr/), 
the [guide](https://github.com/openrndr/openrndr-guide/) and the [template](https://github.com/openrndr/openrndr-template/).

Please read the [general information about contributing to OPENRNDR](https://github.com/openrndr/openrndr/blob/master/CONTRIBUTING.md).
This document focuses on specific details about the ORX repository.

## Overview

There are two types of ORX extras: 
- JVM only. Subfolders of `/orx-jvm/`. These run only on Desktop (not in web browsers).
- Multiplatform. Other `/orx-.../` folders. These run both on Desktop and web browsers.

Each orx folder contains a `README.md`, a `build.gradle.kts` file and a `src` folder. 
Please explore several orx directories to get a feel for how they look like.

Gradle tasks are used to update the list of ORX'es in the root README.md, 
and to update the list of demos in each ORX'es README.md.

## Folder structure (JVM)

```
orx-magic/
├── README.md
├── build.gradle.kts
└── src/
    ├── main/
    │   └── kotlin/
    │       └── Magic.kt
    └── demo/
        └── kotlin/
            ├── DemoFoo01.kt
            └── DemoBar01.kt
```

## Folder structure (multiplatform)

```
orx-magic/
├── README.md
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/
    │   └── Magic.kt
    ├── commonTest/kotlin/
    ├── jsMain/kotlin/
    ├── jsTest/kotlin/
    ├── jvmDemo/kotlin/
    │   ├── DemoFoo01.kt
    │   └── DemoBar01.kt
    ├── jvmMain/kotlin/
    └── jvmTest/kotlin/
```
Note that inside `src` only `commonMain` is required.


## ORX README.md

Assuming you are creating an orx called `magic`, the readme should be formatted as follows:

```
# orx-magic

One or more lines including a short description to display on the root README.md.
One or more lines including a short description to display on the root README.md.
One or more lines including a short description to display on the root README.md.

Main content describing the usage of orx-magic goes here
...

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


## ORX build.gradle.kts

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

The multiplatform build files may have blocks like `commonMain`, `commonTest`, `jvmTest`, `jvmDemo`, etc. to specify the dependencies for each case. See an [example](https://github.com/openrndr/orx/blob/master/orx-color/build.gradle.kts).


## I want to contribute to the documentation

There are various places where you can contribute without writing code. It will be greatly
appreciated by others trying to learn about OPENRNDR.

### Guide

The [guide](https://guide.openrndr.org/) is the first contact with OPENRNDR for most users.
[Learn how to work on the guide](https://github.com/openrndr/openrndr-guide/blob/dev/contributing.md).

### ORX API page

The [ORX API page](https://orx.openrndr.org/) needs some love too. The content is automatically
extracted from comments written in ORX's source code. It goes like this:

1. Fork the [ORX repo](https://github.com/openrndr/orx/), then clone your fork (so you
   have a copy on your computer) and get familiar with OPENRNDR and ORX.
2. Find an undocumented section at https://orx.openrndr.org you want to explain.
3. Find the corresponding Kotlin file in your cloned repo and add missing comments. Read about
   the [suggested style](https://developers.google.com/style).
4. Generate the API website locally to verify your changes look correct by running the following
   command: `./gradlew dokkaGenerate -Dorg.gradle.jvmargs=-Xmx1536M`. This will create the
   html documentation under `build/dokka/html/`.
5. Open the `build/dokka/html/index.html` in your web browser. If something looks off
   tweak your comments. Note: the sidebar will be empty unless viewed through a web server.
   You can launch one by running `python3 -m http.server --bind 127.0.0.1` in the html folder.
7. To continue improving the API go back to step 3, otherwise send a Pull Requests from your fork.


## Demos

ORX'es often include a `jvmDemo` folder. This folder should contain small programs demonstrating
how the ORX can be used. When the build system runs the 
[`CollectScreenShots`](buildSrc/src/main/kotlin/CollectScreenShots.kt) task, 
the `SingleScreenshot()` extension will be injected into each program found inside the `jvmDemo`
folder, then executed. A PNG screenshot is saved and pushed into the [`media`](https://github.com/openrndr/orx/tree/media) brach. Finally, links to those PNG images are inserted into the README.md file of each ORX,
together with a link to the source code that produced the screenshot.

This serves two purposes: it can be useful for the user to see images of what the ORX can produce,
while it can also be usefu to detect breaking changes (in case the demo fails to run, or produces a
blank image).

## Gradle tasks

* `CollectScreenShots`
* `buildMainReadme`

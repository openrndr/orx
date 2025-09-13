plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-parameters"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-shader-phrases"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-color"))
    demoImplementation(project(":orx-composition"))
    demoImplementation(project(":orx-shapes"))
    demoImplementation(project(":orx-svg"))
    demoImplementation(sharedLibs.slf4j.simple)
    demoImplementation(openrndr.ffmpeg)
}

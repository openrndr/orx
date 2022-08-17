import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

sourceSets {
    val demo by getting
    collectScreenshots(project, demo) { }
}

dependencies {
    api(libs.boofcv)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}
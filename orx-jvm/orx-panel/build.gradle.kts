import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.convention.`kotlin-jvm`
}

sourceSets {
    val demo by getting
    collectScreenshots(project, demo) { }
}

dependencies {
    implementation(libs.kotlin.reflect)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.dialogs)
    demoImplementation(libs.gson)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}
import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.convention.`kotlin-jvm`
}

sourceSets {
    val demo by getting
    collectScreenshots(project, demo) { }
}

dependencies {
    implementation(project(":orx-fx"))
    implementation(project(":orx-noise"))
    implementation(openrndrLibs.openrndr.filter)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(libs.slf4j.simple)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}
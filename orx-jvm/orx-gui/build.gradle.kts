import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.convention.`kotlin-jvm`
}

sourceSets {
    val demo by getting
    collectScreenshots(project, demo) { }
}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))
    api(project(":orx-noise"))
    implementation(openrndrLibs.openrndr.filter)
    implementation(openrndrLibs.openrndr.dialogs)
    implementation(libs.gson)
    implementation(libs.kotlin.reflect)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(libs.slf4j.simple)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}
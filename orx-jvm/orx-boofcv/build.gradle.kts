import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

sourceSets {
    val demo by getting
    collectScreenshots(project, demo) { }
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    api(libs.boofcv)
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoRuntimeOnly(libs.openrndr.gl3.core)
}
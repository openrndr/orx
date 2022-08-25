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
    implementation(project(":orx-fx"))
    implementation(project(":orx-noise"))
    implementation(libs.openrndr.filter)
    demoRuntimeOnly(libs.slf4j.simple)
}
import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

sourceSets {
    val demo by getting
    collectScreenshots(project, demo) { }
}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))
    api(project(":orx-noise"))
    demoImplementation(project(":orx-property-watchers"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.openrndr.filter)
    implementation(libs.openrndr.dialogs)
    implementation(libs.gson)
    implementation(libs.kotlin.reflect)
    demoRuntimeOnly(libs.slf4j.simple)
}
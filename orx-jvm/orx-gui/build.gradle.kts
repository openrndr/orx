plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))
    api(project(":orx-noise"))
    demoImplementation(project(":orx-property-watchers"))
    implementation(sharedLibs.kotlin.coroutines)
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(openrndr.filter)
    implementation(openrndr.dialogs)
    implementation(libs.gson)
    implementation(sharedLibs.kotlin.reflect)
    implementation(sharedLibs.kotlin.serialization.core)
    demoRuntimeOnly(sharedLibs.slf4j.simple)
}
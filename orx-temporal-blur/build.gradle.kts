plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(project(":orx-color"))
    implementation(project(":orx-noise"))
    implementation(project(":orx-fx"))
    implementation(openrndr.filter)
}
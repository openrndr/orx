plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(project(":orx-fx"))
    implementation(project(":orx-noise"))
    implementation(openrndr.filter)
    demoRuntimeOnly(sharedLibs.slf4j.simple)
}
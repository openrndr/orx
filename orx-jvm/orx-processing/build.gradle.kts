plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    api(libs.processing.core) {
        exclude(group = "org.jogamp.gluegen")
        exclude(group = "org.jogamp.jogl")
    }
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.reflect)
    demoRuntimeOnly(sharedLibs.slf4j.simple)
    demoImplementation(project(":orx-shapes"))
}
plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    api(libs.processing.core) {
        exclude(group = "org.jogamp.gluegen")
        exclude(group = "org.jogamp.jogl")
    }
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.reflect)
    demoRuntimeOnly(libs.slf4j.simple)
    demoImplementation(project(":orx-shapes"))
}
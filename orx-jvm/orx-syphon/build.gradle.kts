plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application)
    implementation(libs.openrndr.gl3.core)
}
plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(sharedLibs.kotlin.serialization.json)
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(libs.okhttp)
}
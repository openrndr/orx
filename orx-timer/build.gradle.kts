plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.coroutines)
}
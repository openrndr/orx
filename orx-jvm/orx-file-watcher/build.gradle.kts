plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.coroutines)
}
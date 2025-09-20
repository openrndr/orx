plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.reflect)
    implementation(sharedLibs.kotlin.coroutines)
    implementation(project(":orx-property-watchers"))
    implementation(project(":orx-parameters"))

    testImplementation(libs.mockk)
    testImplementation(sharedLibs.kotest.assertions)
}

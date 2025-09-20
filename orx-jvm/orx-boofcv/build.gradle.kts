plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    demoImplementation(project(":orx-shapes"))
    api(libs.boofcv)
}
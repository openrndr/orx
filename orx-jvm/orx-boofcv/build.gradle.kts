plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    demoImplementation(project(":orx-shapes"))
    demoImplementation(openrndr.bundles.video)
    api(libs.boofcv)
}
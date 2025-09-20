plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":orx-fx"))
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    demoImplementation(project(":orx-image-fit"))
}
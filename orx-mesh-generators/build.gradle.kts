plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    demoImplementation(project(":orx-shapes"))
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-camera"))
}
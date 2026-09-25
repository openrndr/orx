plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    demoImplementation(project(":orx-shapes"))

    implementation(libs.onnx.runtime)
}

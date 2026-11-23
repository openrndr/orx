plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":orx-jvm:orx-onnx-runtime"))
    implementation(project(":orx-jvm:orx-python"))
    demoImplementation(project(":orx-shapes"))
}

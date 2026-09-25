plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    demoImplementation(project(":orx-shapes"))
    implementation(libs.onnx.runtime)
    implementation(project(":orx-jvm:orx-onnx-runtime"))
    implementation(project(":orx-jvm:orx-python"))
}

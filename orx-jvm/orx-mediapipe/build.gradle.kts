plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(project(":orx-jvm:orx-onnx-runtime"))
    demoImplementation(openrndr.bundles.video)
}

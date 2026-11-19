plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(project(":orx-jvm:orx-onnx-runtime"))
    demoImplementation(openrndr.bundles.video)
}

tasks.named<CollectScreenshotsTask>("collectScreenshots") {
    // the demos that use the webcam, which a screenshot cannot show
    ignore.set(fileTree("src/demo/kotlin").filter { "loadVideoDevice" in it.readText() }.map { it.nameWithoutExtension })
}

plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(libs.gson)
    implementation(sharedLibs.kotlin.reflect)
    implementation(project(":orx-noise"))
    implementation(project(":orx-easing"))
    api(project(":orx-expression-evaluator"))
    demoImplementation(project(":orx-jvm:orx-panel"))
}

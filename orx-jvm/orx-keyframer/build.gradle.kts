plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.gson)
    implementation(libs.kotlin.reflect)
    implementation(project(":orx-noise"))
    implementation(project(":orx-easing"))
    api(project(":orx-expression-evaluator"))
    demoImplementation(project(":orx-jvm:orx-panel"))
}

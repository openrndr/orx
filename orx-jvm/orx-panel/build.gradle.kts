plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

tasks.test {
    useJUnitPlatform {
    }
}

dependencies {
    implementation(project(":orx-expression-evaluator"))
    implementation(project(":orx-color"))
    implementation(project(":orx-text-writer"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.reflect)
    testRuntimeOnly(libs.kotlin.reflect)
    demoImplementation(libs.openrndr.dialogs)
    demoImplementation(libs.gson)
    demoImplementation(project(":orx-jvm:orx-panel"))
}
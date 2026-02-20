plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

tasks.test {
    useJUnitPlatform {
    }
}

dependencies {
    implementation(project(":orx-expression-evaluator"))
    implementation(project(":orx-color"))
    implementation(project(":orx-text-writer"))
    implementation(project(":orx-view-box"))
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.coroutines)
    implementation(sharedLibs.kotlin.reflect)
    testRuntimeOnly(sharedLibs.kotlin.reflect)
    demoImplementation(openrndr.dialogs)
    demoImplementation(libs.gson)
    demoImplementation(project(":orx-jvm:orx-panel"))
    demoImplementation(project(":orx-shapes"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(sharedLibs.kotlin.serialization.json)
}
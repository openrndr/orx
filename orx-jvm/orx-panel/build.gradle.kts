plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

tasks.test {
    useJUnitPlatform {
    }
}

dependencies {
    implementation(project(":orx-expression-evaluator"))
    implementation(project(":orx-color"))
    implementation(project(":orx-text-writer"))
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.coroutines)
    implementation(sharedLibs.kotlin.reflect)
    testRuntimeOnly(sharedLibs.kotlin.reflect)
    demoImplementation(openrndr.dialogs)
    demoImplementation(libs.gson)
    demoImplementation(project(":orx-jvm:orx-panel"))
}
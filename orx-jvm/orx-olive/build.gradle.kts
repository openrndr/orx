plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

tasks.test {
    useJUnitPlatform {
    }
}

dependencies {
    implementation(project(":orx-jvm:orx-file-watcher"))
    implementation(project(":orx-jvm:orx-kotlin-parser"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(libs.kotlin.scriptingJvm)
    implementation(libs.kotlin.scriptingJvmHost)
    implementation(sharedLibs.kotlin.reflect)
    implementation(libs.kotlin.scriptingJSR223)
    implementation(sharedLibs.kotlin.coroutines)
    demoImplementation(sharedLibs.kotlin.coroutines)
    testImplementation(sharedLibs.kotest.runner)
    testImplementation(sharedLibs.kotest.assertions)
    testRuntimeOnly(sharedLibs.kotlin.reflect)
}
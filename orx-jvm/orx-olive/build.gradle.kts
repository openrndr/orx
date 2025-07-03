import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.test {
    useJUnitPlatform {
    }
}

dependencies {
    implementation(project(":orx-jvm:orx-file-watcher"))
    implementation(project(":orx-jvm:orx-kotlin-parser"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.scriptingJvm)
    implementation(libs.kotlin.scriptingJvmHost)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.scriptingJSR223)
    implementation(libs.kotlin.coroutines)
    demoImplementation(libs.kotlin.coroutines)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.kotlin.reflect)
}
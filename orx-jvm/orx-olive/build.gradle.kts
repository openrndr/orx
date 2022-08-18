import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
}

tasks.test {
    useJUnitPlatform {
        includeEngines("spek2")
    }
}

dependencies {
    implementation(project(":orx-jvm:orx-file-watcher"))
    implementation(project(":orx-jvm:orx-kotlin-parser"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.scriptingJvm)
    implementation(libs.kotlin.scriptingJvmHost)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.scriptingJSR223)
    testImplementation(libs.kluent)
    testImplementation(libs.spek.dsl)
    testRuntimeOnly(libs.spek.junit5)
    demoImplementation(project(":orx-camera"))
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoRuntimeOnly(libs.openrndr.gl3.core)
}
import ScreenshotsHelper.collectScreenshots
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

sourceSets {
    val demo by getting
    collectScreenshots(project, demo) { }
}

dependencies {
    implementation(libs.kotlin.reflect)
    testImplementation(libs.spek.dsl)
    testRuntimeOnly(libs.spek.junit5)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.dialogs)
    demoImplementation(libs.gson)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}
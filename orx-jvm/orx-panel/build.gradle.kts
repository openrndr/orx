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
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.spek.dsl)
    testRuntimeOnly(libs.spek.junit5)
    testRuntimeOnly(libs.kotlin.reflect)
    demoImplementation(libs.openrndr.dialogs)
    demoImplementation(libs.gson)
}
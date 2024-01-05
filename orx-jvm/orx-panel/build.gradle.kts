import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
}

tasks.test {
    useJUnitPlatform {
    }
}

dependencies {
    implementation(project(":orx-expression-evaluator"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.reflect)
    testRuntimeOnly(libs.kotlin.reflect)
    demoImplementation(libs.openrndr.dialogs)
    demoImplementation(libs.gson)
}
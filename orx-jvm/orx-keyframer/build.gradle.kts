import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
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
    testImplementation(libs.kluent)
}

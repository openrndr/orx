plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.coroutines)
    implementation(project(":orx-property-watchers"))
    implementation(project(":orx-parameters"))
}
plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(project(":orx-fx"))
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
}
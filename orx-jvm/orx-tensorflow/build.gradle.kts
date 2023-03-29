plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.gson)
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    api(libs.tensorflow)
}
plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    api(project(":orx-noise"))
    implementation(libs.openrndr.shape)
    implementation(libs.openrndr.math)
    implementation(libs.delaunator)
}
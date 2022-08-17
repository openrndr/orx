plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.gson)
    implementation(libs.jarchivelib)
    implementation(project(":orx-noise"))
}
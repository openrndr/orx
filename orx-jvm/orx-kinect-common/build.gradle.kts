plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}
dependencies {
    api(project(":orx-depth-camera"))
    implementation(openrndr.application)
    implementation(openrndr.math)
}
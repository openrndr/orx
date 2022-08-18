plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    api(project(":orx-jvm:orx-kinect-common"))
    api(libs.libfreenect)
}
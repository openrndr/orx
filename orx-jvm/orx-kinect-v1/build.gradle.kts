plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.coroutines)
    api(project(":orx-jvm:orx-kinect-common"))
    api(libs.libfreenect)
}
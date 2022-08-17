plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.librealsense) { classifier("linux-x86_64") })
}
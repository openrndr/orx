plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.librealsense) { classifier("windows-x86_64") })
}
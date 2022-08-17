plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.librealsense) { classifier("macosx-x86_64") })
}
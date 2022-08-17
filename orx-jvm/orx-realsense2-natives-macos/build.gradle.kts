plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.librealsense) { classifier("macosx-x86_64") })
}
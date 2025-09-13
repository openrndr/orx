plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}
dependencies {
    runtimeOnly(variantOf(libs.librealsense) { classifier("linux-x86_64") })
}
plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}
dependencies {
    runtimeOnly(variantOf(libs.libfreenect) { classifier("linux-x86_64") })
}
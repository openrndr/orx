plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.libfreenect) { classifier("windows-x86_64") })
}
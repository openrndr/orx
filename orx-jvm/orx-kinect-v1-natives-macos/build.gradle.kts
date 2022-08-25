plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.libfreenect) { classifier("macosx-x86_64") })
}
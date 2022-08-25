plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.tensorflow) { classifier("linux-x86_64") })
}
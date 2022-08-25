plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.tensorflow) { classifier("windows-x86_64") })
}
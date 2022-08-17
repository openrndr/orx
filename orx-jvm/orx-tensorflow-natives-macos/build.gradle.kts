plugins {
    org.openrndr.convention.`kotlin-jvm`
}
dependencies {
    runtimeOnly(variantOf(libs.tensorflow) { classifier("macosx-x86_64") })
}
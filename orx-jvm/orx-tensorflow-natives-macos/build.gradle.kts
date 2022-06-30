dependencies {
    runtimeOnly(variantOf(libs.tensorflow) { classifier("macosx-x86_64") })
}
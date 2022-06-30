dependencies {
    runtimeOnly(variantOf(libs.tensorflow) { classifier("windows-x86_64-gpu") })
}
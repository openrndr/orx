dependencies {
    implementation(libs.javaosc.core) {
        exclude(group = "org.slf4j")
        exclude(group = "log4j")
    }
}

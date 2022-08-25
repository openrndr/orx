plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.javaosc.core) {
        exclude(group = "org.slf4j")
        exclude(group = "log4j")
    }
}
plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":orx-parameters"))
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(libs.javaosc.core) {
        exclude(group = "org.slf4j")
        exclude(group = "log4j")
    }
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.gson)
}

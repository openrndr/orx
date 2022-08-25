plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))
    api(libs.minim) {
        exclude(group = "org.apache.maven.plugins", module = "maven-javadoc-plugin")
    }
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.reflect)
    demoRuntimeOnly(libs.slf4j.simple)
}
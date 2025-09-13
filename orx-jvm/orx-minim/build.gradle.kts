plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))
    api(libs.minim) {
        exclude(group = "org.apache.maven.plugins", module = "maven-javadoc-plugin")
    }
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.reflect)
    demoRuntimeOnly(sharedLibs.slf4j.simple)
}
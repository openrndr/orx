plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))
    api(libs.minim) {
        exclude(group = "org.apache.maven.plugins", module = "maven-javadoc-plugin")
    }
    implementation(libs.kotlin.reflect)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(libs.slf4j.simple)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}
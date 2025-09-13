plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    implementation(project(":orx-fx"))
    implementation(project(":orx-noise"))
    implementation(openrndr.filter)
    demoRuntimeOnly(sharedLibs.slf4j.simple)
}
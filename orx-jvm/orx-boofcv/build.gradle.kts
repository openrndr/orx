plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    demoImplementation(project(":orx-shapes"))
    testRuntimeOnly(libs.openrndr.nullgl)
    api(libs.boofcv)
}
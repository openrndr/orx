plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.math)
    demoImplementation(project(":orx-shapes"))
    api(libs.boofcv)
}
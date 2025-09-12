plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(project(":orx-fx"))
    implementation(openrndr.application)
    implementation(openrndr.math)
    demoImplementation(project(":orx-image-fit"))
}
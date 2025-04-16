plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.dialogs)
    implementation(project(":orx-jvm:orx-gui"))
    implementation(project(":orx-composition"))
    implementation(project(":orx-svg"))
    implementation(project(":orx-image-fit"))
    implementation(project(":orx-shapes"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-parameters"))
}

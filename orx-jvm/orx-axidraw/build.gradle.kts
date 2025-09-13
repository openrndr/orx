plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(openrndr.application)
    implementation(openrndr.dialogs)
    implementation(project(":orx-jvm:orx-gui"))
    api(project(":orx-composition"))
    implementation(project(":orx-svg"))
    implementation(project(":orx-image-fit"))
    implementation(project(":orx-shapes"))
    implementation(project(":orx-camera"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-parameters"))
    demoImplementation(project(":orx-jvm:orx-axidraw"))
}

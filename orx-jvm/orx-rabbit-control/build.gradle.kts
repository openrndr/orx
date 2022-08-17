plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(project(":orx-parameters"))
    implementation(project(":orx-compositor"))
    implementation(project(":orx-image-fit"))
    implementation(project(":orx-fx"))
    implementation(libs.rabbitcontrol.rcp)
    implementation(libs.netty.all)
    implementation(libs.zxing.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
}
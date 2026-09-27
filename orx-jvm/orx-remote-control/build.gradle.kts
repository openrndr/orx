plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":orx-parameters"))
    implementation(project(":orx-compositor"))
    implementation(project(":orx-image-fit"))
    implementation(project(":orx-fx"))
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(libs.rabbitcontrol.rcp)
    implementation(libs.netty.all)
    implementation(libs.zxing.core)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
}
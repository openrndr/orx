dependencies {
    implementation(project(":orx-jvm:orx-kinect-v1"))
    runtimeOnly(project(":orx-jvm:orx-kinect-v1-${(gradle as ExtensionAware).extra["openrndrClassifier"]}"))
    runtimeOnly(libs.openrndr.gl3)
    runtimeOnly(libs.openrndr.gl3.natives)
    runtimeOnly("ch.qos.logback:logback-classic:1.2.11")
}

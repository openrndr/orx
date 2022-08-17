import ScreenshotsHelper.collectScreenshots

sourceSets {
    val main by getting { }
    val demo by creating {
        java {
            srcDir("src/demo/kotlin")
            compileClasspath += main.getCompileClasspath()
            runtimeClasspath += main.getRuntimeClasspath()
        }
    }
    collectScreenshots(project, demo) { }
}

val demoImplementation by configurations.getting {}
val demoRuntimeOnly by configurations.getting {}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))
    api(project(":orx-noise"))
    implementation(openrndrLibs.openrndr.filter)
    implementation(openrndrLibs.openrndr.dialogs)
    implementation(libs.gson)
    implementation(libs.kotlin.reflect)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)

    demoRuntimeOnly(libs.slf4j.simple)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
    // FIXME!!! demoRuntimeOnly(openrndrLibs.openrndr.gl3.natives)
    demoImplementation(sourceSets.getByName("main").output)
}

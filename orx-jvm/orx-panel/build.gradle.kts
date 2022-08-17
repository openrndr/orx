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
    implementation(libs.kotlin.reflect)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoImplementation(openrndrLibs.openrndr.dialogs)
    demoImplementation(libs.gson)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
    // FIXME!!! demoRuntimeOnly(openrndrLibs.openrndr.gl3.natives)
    demoImplementation(sourceSets.getByName("main").output)
}

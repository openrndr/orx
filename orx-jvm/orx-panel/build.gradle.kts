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
    demoImplementation(libs.openrndr.extensions)
    demoImplementation(libs.openrndr.dialogs)
    demoImplementation(libs.gson)
    demoRuntimeOnly(libs.openrndr.gl3)
    demoRuntimeOnly(libs.openrndr.gl3.natives)
    demoImplementation(sourceSets.getByName("main").output)
}

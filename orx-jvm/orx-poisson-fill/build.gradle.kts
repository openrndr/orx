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
    implementation(project(":orx-fx"))
    implementation(project(":orx-noise"))
    implementation(openrndrLibs.openrndr.filter)
    demoImplementation(openrndrLibs.openrndr.application)
    demoImplementation(openrndrLibs.openrndr.extensions)
    demoRuntimeOnly(libs.slf4j.simple)
    demoRuntimeOnly(openrndrLibs.openrndr.gl3.core)
    // FIXME!!! demoRuntimeOnly(openrndrLibs.openrndr.gl3.natives)
    demoImplementation(sourceSets.getByName("main").output)
}
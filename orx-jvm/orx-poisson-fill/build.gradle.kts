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
    implementation(libs.openrndr.filter)
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)
    demoRuntimeOnly(libs.slf4j.simple)
    demoRuntimeOnly(libs.openrndr.gl3)
    demoRuntimeOnly(libs.openrndr.gl3.natives)
    demoImplementation(sourceSets.getByName("main").output)
}
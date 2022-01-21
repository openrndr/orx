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

val openrndrVersion: String by rootProject.extra
val openrndrOS: String by rootProject.extra
val slf4jVersion:String by rootProject.extra

val demoImplementation by configurations.getting {}
val demoRuntimeOnly by configurations.getting {}

dependencies {
    implementation(project(":orx-fx"))
    implementation(project(":orx-noise"))
    implementation("org.openrndr:openrndr-filter:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-application:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")
    demoRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3-natives-$openrndrOS:$openrndrVersion")
    demoImplementation(sourceSets.getByName("main").output)
}
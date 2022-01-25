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
val kotlinVersion:String by rootProject.extra
val gsonVersion:String by rootProject.extra
val demoImplementation by configurations.getting {}

val demoRuntimeOnly by configurations.getting {}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    demoImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-dialogs:$openrndrVersion")
    demoImplementation("com.google.code.gson:gson:$gsonVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3-natives-$openrndrOS:$openrndrVersion")
    demoImplementation(sourceSets.getByName("main").output)
}

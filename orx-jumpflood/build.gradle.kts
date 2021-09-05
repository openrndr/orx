sourceSets {
    val main by getting { }
    val demo by creating {
        java {
            srcDir("src/demo/kotlin")
            compileClasspath += main.getCompileClasspath()
            runtimeClasspath += main.getRuntimeClasspath()
        }
    }
}


val openrndrVersion: String by rootProject.extra
val openrndrOS: String by rootProject.extra

val demoImplementation by configurations.getting {}
val demoRuntimeOnly by configurations.getting {}


dependencies {
    implementation(project(":orx-fx"))
    implementation(project(":orx-parameters"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-compositor"))
    demoImplementation("org.openrndr:openrndr-application:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-svg:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-ffmpeg:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3-natives-$openrndrOS:$openrndrVersion")
    demoImplementation(sourceSets.getByName("main").output)
}
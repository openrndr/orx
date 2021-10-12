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
    demoImplementation(project(":orx-mesh-generators"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(project(":orx-parameters"))
    demoImplementation(project(":orx-noise"))
    demoImplementation(project(":orx-jvm:orx-gui"))
    demoImplementation(project(":orx-shader-phrases"))
    demoImplementation("org.slf4j:slf4j-simple:1.7.30")

    demoImplementation("org.openrndr:openrndr-application:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-ffmpeg:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-svg:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-ffmpeg-natives-$openrndrOS:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3-natives-$openrndrOS:$openrndrVersion")
    demoImplementation(sourceSets.getByName("main").output)
}

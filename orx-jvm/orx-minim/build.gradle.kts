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
val slf4jVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra
val demoImplementation by configurations.getting {}
val demoRuntimeOnly by configurations.getting {}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))

    api("net.compartmental.code:minim:2.2.2") {
        exclude(group = "org.apache.maven.plugins", module = "maven-javadoc-plugin")
    }
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    demoImplementation("org.openrndr:openrndr-application:$openrndrVersion")
    demoImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")

    demoRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3:$openrndrVersion")
    demoRuntimeOnly("org.openrndr:openrndr-gl3-natives-$openrndrOS:$openrndrVersion")
    demoImplementation(sourceSets.getByName("main").output)
}

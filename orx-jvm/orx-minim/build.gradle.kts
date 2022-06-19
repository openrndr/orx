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

val demoImplementation by configurations.getting {}
val demoRuntimeOnly by configurations.getting {}

dependencies {
    api(project(":orx-parameters"))
    api(project(":orx-jvm:orx-panel"))

    api("net.compartmental.code:minim:2.2.2") {
        exclude(group = "org.apache.maven.plugins", module = "maven-javadoc-plugin")
    }
    implementation(libs.kotlin.reflect)
    demoImplementation(libs.openrndr.application)
    demoImplementation(libs.openrndr.extensions)

    demoRuntimeOnly(libs.slf4j.simple)
    demoRuntimeOnly(libs.openrndr.gl3)
    demoRuntimeOnly(libs.openrndr.gl3.natives)
    demoImplementation(sourceSets.getByName("main").output)
}

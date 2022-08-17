plugins {
    `kotlin-dsl`
}

sourceSets {
    val preload by creating {
        this.java {
            srcDir("src/preload/kotlin")
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

val openrndrVersion =
    (findProperty("OPENRNDR.version")?.toString() ?: System.getenv("OPENRNDR_VERSION"))?.replace("v", "")
        ?: "0.5.1-SNAPSHOT"

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    val preloadImplementation by configurations.getting
    preloadImplementation("org.openrndr:openrndr-application:$openrndrVersion")
    preloadImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")
}

tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")
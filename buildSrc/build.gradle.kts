plugins {
    `kotlin-dsl`
}

val preload: SourceSet by sourceSets.creating

repositories {
    mavenCentral()
    mavenLocal()
}

val openrndrVersion: String =
    (extra.properties["OPENRNDR.version"] as String? ?: System.getenv("OPENRNDR_VERSION"))?.removePrefix("v")
        ?: "0.5.1-SNAPSHOT"

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.openrndr") useVersion(openrndrVersion)
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    "preloadImplementation"(libs.openrndr.application)
    "preloadImplementation"(libs.openrndr.extensions)
}

tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")
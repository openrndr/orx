plugins {
    `kotlin-dsl`
}

val preload: SourceSet by sourceSets.creating

repositories {
    mavenCentral()
    mavenLocal()
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
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")
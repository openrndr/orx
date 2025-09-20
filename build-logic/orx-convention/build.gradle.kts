plugins {
    `kotlin-dsl`
}

val preload: SourceSet by project.sourceSets.creating

repositories {
    mavenCentral()
    mavenLocal()
}
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(project(":orx-variant-plugin"))
    implementation(libs.findLibrary("kotlin-gradle-plugin").get())
    implementation(libs.findLibrary("dokka-gradle-plugin").get())
    "preloadImplementation"(openrndr.application.core)
    "preloadImplementation"(openrndr.orextensions)
}
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")

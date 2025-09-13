plugins {
    `kotlin-dsl`
}

val preload: SourceSet by sourceSets.creating


repositories {
    mavenCentral()
    mavenLocal()
}
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(libs.findLibrary("kotlin-gradle-plugin").get())
    implementation(libs.findLibrary("dokka-gradle-plugin").get())
    "preloadImplementation"(openrndr.application)
    "preloadImplementation"(openrndr.orextensions)
}

tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")
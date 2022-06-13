
plugins {
    `kotlin-dsl`
}

sourceSets {
    val preload by creating {
        this.java {
            srcDir("src/preload/kotlin")
        }
    }
    val main by getting {
    }

}

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}


val openrndrVersion = ((findProperty("OPENRNDR.version")?.toString())?:System.getenv("OPENRNDR_VERSION"))?.replace("v", "")  ?: "0.5.1-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.20")
    val preloadImplementation by configurations.getting {  }
    preloadImplementation("org.openrndr:openrndr-application:$openrndrVersion")
    preloadImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")
}

tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")

// Here we deal with Gradle 7.4.2 using Kotlin 1.5 while OPENRNDR is compiled using Kotlin 1.7
// This seems to work at this point in time but may cause serious problems in future scenarios in which either Kotlin,
// Gradle or OPENRNDR are upgraded.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xskip-metadata-version-check")
}
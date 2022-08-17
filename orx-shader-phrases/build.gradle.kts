plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    kotlin("plugin.serialization")
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(file("$buildDir/generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.shaderphrases.phrases")
}.get()

kotlin {
    jvm {
        testRuns["test"].executionTask {
            useJUnitPlatform {
                includeEngines("spek2")
            }
        }
    }
    sourceSets {
        val shaderKotlin by creating {
            this.kotlin.srcDir(embedShaders.outputDir)
        }

        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.serialization.core)
                implementation(openrndrLibs.openrndr.application)
                implementation(openrndrLibs.openrndr.draw)
                implementation(libs.kotlin.reflect)
                api(shaderKotlin.kotlin)
            }
            dependsOn(shaderKotlin)
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
                runtimeOnly(libs.slf4j.simple)
                implementation(libs.kluent)
                implementation(libs.spek.dsl)
                runtimeOnly(libs.spek.junit5)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(openrndrLibs.openrndr.application)
                implementation(openrndrLibs.openrndr.extensions)
                runtimeOnly(openrndrLibs.openrndr.gl3.core)
            }
        }
    }
}
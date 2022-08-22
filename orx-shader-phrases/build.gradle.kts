plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
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
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.kotlin.reflect)
                api(shaderKotlin.kotlin)
            }
            dependsOn(shaderKotlin)
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.slf4j.simple)
                implementation(libs.kluent)
                implementation(libs.spek.dsl)
                runtimeOnly(libs.spek.junit5)
                runtimeOnly(libs.kotlin.reflect)
            }
        }
    }
}
plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(file("$buildDir/generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.noise.filters")
    defaultVisibility.set("internal")
    namePrefix.set("noise_")
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
                implementation(libs.openrndr.math)
                implementation(libs.openrndr.shape)
                implementation(libs.openrndr.draw)
                implementation(project(":orx-hash-grid"))
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                api(shaderKotlin.kotlin)
            }
            dependsOn(shaderKotlin)
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(libs.spek.dsl)
                runtimeOnly(libs.spek.junit5)
                runtimeOnly(libs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-hash-grid"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-jvm:orx-gui"))
            }
        }
    }
}
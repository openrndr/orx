import ScreenshotsHelper.collectScreenshots

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations {
            val demo by creating {
                defaultSourceSet {
                    kotlin.srcDir("src/demo")
                    dependencies {
                        implementation(project(":orx-shapes"))
                        implementation(project(":orx-noise"))
                        implementation(project(":orx-triangulation"))
                        implementation(libs.openrndr.application)
                        implementation(libs.openrndr.shape)
                        implementation(libs.openrndr.extensions)
                        runtimeOnly(libs.openrndr.gl3.core)
                        runtimeOnly(libs.openrndr.gl3.natives)
                        implementation(compilations["main"]!!.output.allOutputs)
                    }
                }
                collectScreenshots {

                }
            }
        }
        compilations.all {
            kotlinOptions.jvmTarget = libs.versions.jvmTarget.get()
            kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(libs.openrndr.math)
                api(libs.openrndr.shape)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotest)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
                implementation(libs.kotlin.serialization.json)
                runtimeOnly(libs.bundles.jupiter)
                implementation(libs.spek.dsl)
                implementation(libs.kluent)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
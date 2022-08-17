import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        @Suppress("UNUSED_VARIABLE")
        val demo by compilations.getting {
            // TODO: Move demos to /jvmDemo
            defaultSourceSet {
                kotlin.srcDir("src/demo/kotlin")
            }
            collectScreenshots {  }
        }
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-fx"))
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(libs.kotlin.serialization.core)
                implementation(openrndrLibs.openrndr.application)
                implementation(openrndrLibs.openrndr.draw)
                implementation(openrndrLibs.openrndr.filter)
                implementation(libs.kotlin.reflect)
            }
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
                implementation(libs.spek.dsl)
                implementation(libs.kluent)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-fx"))
                implementation(project(":orx-camera"))
                implementation(openrndrLibs.openrndr.application)
                implementation(openrndrLibs.openrndr.extensions)
                runtimeOnly(openrndrLibs.openrndr.gl3.core)
            }
        }
    }
}
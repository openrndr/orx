import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    jvm {
        @Suppress("UNUSED_VARIABLE")
        val demo by compilations.getting {
            collectScreenshots { }
        }
        testRuns["test"].executionTask {
            useJUnitPlatform {
                includeEngines("spek2")
            }
        }
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(project(":orx-color"))
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.filter)
                implementation(libs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(project(":orx-triangulation"))
            }
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(libs.kluent)
                implementation(libs.spek.dsl)
                runtimeOnly(libs.spek.junit5)
                runtimeOnly(libs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-color"))
                implementation(project(":orx-triangulation"))
                implementation(project(":orx-shapes"))
                implementation(project(":orx-noise"))
            }
        }
    }
}
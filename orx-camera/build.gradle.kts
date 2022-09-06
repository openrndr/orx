import ScreenshotsHelper.collectScreenshots

plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    jvm {
        @Suppress("UNUSED_VARIABLE")
        val demo by compilations.getting {
            // TODO: Move demos to /jvmDemo
            defaultSourceSet {
                kotlin.srcDir("src/demo/kotlin")
            }
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
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.filter)
                implementation(libs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-mesh-generators"))
            }
        }
    }
}

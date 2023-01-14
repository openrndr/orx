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
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-fx"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-view-box"))
            }
        }
    }
}

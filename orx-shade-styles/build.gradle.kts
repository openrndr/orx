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
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-color"))
                implementation(project(":orx-shade-styles"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-shapes"))
            }
        }
    }
}
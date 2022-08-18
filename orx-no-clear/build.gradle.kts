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
        }
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.serialization.core)
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                implementation(libs.openrndr.shape)
                implementation(libs.openrndr.draw)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
            }
        }
    }
}
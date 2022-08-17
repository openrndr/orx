plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(openrndrLibs.openrndr.application)
                implementation(openrndrLibs.openrndr.math)
                implementation(libs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotest)
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
    }
}
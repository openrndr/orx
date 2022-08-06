plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val shaderKotlin by creating {
            this.kotlin.srcDir("$projectDir/build/generated/shaderKotlin")
        }

        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.logging)
            }
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
        val jvmMain by getting

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
        val jsMain by getting

        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

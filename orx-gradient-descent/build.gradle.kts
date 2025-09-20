plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
    jvm {
        testRuns["test"].executionTask {
            useJUnitPlatform {

            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(openrndr.application.core)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(sharedLibs.kotest.assertions)
                implementation(sharedLibs.kotest.framework.engine)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(sharedLibs.kotest.assertions)
                implementation(sharedLibs.kotest.framework.engine)
                runtimeOnly(sharedLibs.kotlin.reflect)
            }
        }
    }
}
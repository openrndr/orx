plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
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
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.filter)
                implementation(libs.kotlin.reflect)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.framework.engine)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.framework.engine)
                runtimeOnly(libs.kotlin.reflect)
            }
        }
    }
}
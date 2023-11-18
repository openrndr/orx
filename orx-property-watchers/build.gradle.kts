plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    jvm {
        testRuns["test"].executionTask {
            useJUnitPlatform {
            }
        }
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                implementation(libs.kotlin.reflect)
            }
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(libs.kluent)
                runtimeOnly(libs.kotlin.reflect)
            }
        }
    }
}
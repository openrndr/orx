plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
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
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(openrndr.application.core)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
                implementation(sharedLibs.kotlin.coroutines)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo = getByName("jvmDemo") {
            dependencies {
                implementation(project(":orx-noise"))
            }
        }
    }
}

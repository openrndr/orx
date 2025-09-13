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
                implementation(openrndr.application)
                implementation(openrndr.draw)
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

plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    jvm {
        @Suppress("UNUSED_VARIABLE")
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(openrndr.application.core)
                implementation(openrndr.draw)
                implementation(openrndr.shape)
                api(project(":orx-composition"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies { }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies { }
        }
    }
}

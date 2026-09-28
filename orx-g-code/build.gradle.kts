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
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
            implementation(openrndr.shape)
            api(project(":orx-composition"))
        }

        jvmTest.dependencies {}

        jvmDemo.dependencies {}
    }
}

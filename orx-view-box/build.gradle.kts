plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    jvm {
        testRuns["test"].executionTask {
            useJUnitPlatform {}
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-camera"))
            implementation(project(":orx-fx"))
            implementation(project(":orx-mesh-generators"))
            implementation(project(":orx-view-box"))
            implementation(project(":orx-shapes"))
        }
    }
}

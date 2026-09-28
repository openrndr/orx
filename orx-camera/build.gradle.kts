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
            implementation(project(":orx-parameters"))
            implementation(project(":orx-shader-phrases"))
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
            implementation(openrndr.filter)
            implementation(sharedLibs.kotlin.reflect)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-camera"))
            implementation(project(":orx-mesh-generators"))
            implementation(project(":orx-jvm:orx-gui"))
            implementation(project(":orx-jvm:orx-remote-control"))
        }
    }
}

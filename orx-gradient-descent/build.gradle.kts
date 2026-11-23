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

        jvmTest.dependencies {
            runtimeOnly(sharedLibs.kotlin.reflect)
        }
    }
}
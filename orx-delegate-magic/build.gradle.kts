plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":orx-parameters"))
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
            implementation(openrndr.filter)
            implementation(sharedLibs.kotlin.reflect)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-delegate-magic"))
            implementation(project(":orx-jvm:orx-gui"))
        }
    }
}
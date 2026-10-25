plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.shape)
            implementation(openrndr.draw)
            implementation(openrndr.application.core)
            implementation(project(":orx-shapes"))
            api(project(":orx-text-writer"))
        }

        val jvmDemo by getting {
            dependencies {
                implementation(openrndr.ffmpeg)
                implementation(project(":orx-text-on-contour"))
                implementation(project(":orx-camera"))
            }
        }
    }
}
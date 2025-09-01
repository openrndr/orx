plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.shape)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.application)
                implementation(project(":orx-shapes"))
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-text-on-contour"))
            }
        }
    }
}
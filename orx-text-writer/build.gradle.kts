plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(openrndr.shape)
                implementation(openrndr.draw)
                implementation(openrndr.application.core)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-text-writer"))
                implementation(project(":orx-shapes"))
            }
        }
    }
}
plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.shape)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.application)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-text-writer"))
            }
        }
    }
}
plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(openrndr.shape)
                implementation(openrndr.draw)
                implementation(openrndr.application)
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
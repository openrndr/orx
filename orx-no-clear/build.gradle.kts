plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(openrndr.application)
                implementation(openrndr.math)
                implementation(openrndr.shape)
                implementation(openrndr.draw)
            }
        }
    }
}
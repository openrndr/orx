plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.math)
            implementation(openrndr.shape)
            implementation(openrndr.draw)
        }
    }
}
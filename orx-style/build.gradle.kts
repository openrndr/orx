plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(openrndr.color)
            api(openrndr.math)
        }
    }
}

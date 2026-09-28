plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.shape)
            implementation(openrndr.draw)
            implementation(openrndr.application.core)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-text-writer"))
            implementation(project(":orx-shapes"))
        }
    }
}
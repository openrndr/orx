plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
            implementation(openrndr.filter)
            implementation(sharedLibs.kotlin.reflect)
            implementation(sharedLibs.kotlin.serialization.core)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-svg"))
        }
    }
}

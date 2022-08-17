plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(libs.kotlin.serialization.core)
                implementation(openrndrLibs.openrndr.application)
                implementation(openrndrLibs.openrndr.draw)
                implementation(openrndrLibs.openrndr.filter)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}
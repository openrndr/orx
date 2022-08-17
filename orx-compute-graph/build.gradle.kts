plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(openrndrLibs.openrndr.event)
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}
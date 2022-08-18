plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }
        }
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(openrndrLibs.openrndr.event)
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}
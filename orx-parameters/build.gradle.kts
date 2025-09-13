plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
   sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(openrndr.application)
                implementation(openrndr.math)
                implementation(sharedLibs.kotlin.reflect)

            }
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(sharedLibs.kotest.assertions)
                implementation(sharedLibs.kotest.framework.engine)
                implementation(sharedLibs.kotlin.serialization.json)
                runtimeOnly(sharedLibs.kotlin.reflect)
            }
        }
    }
}
plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
   sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                implementation(libs.kotlin.reflect)

            }
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotlin.serialization.json)
                runtimeOnly(libs.kotlin.reflect)
            }
        }
    }
}
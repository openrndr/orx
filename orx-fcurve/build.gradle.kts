plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-expression-evaluator"))
                implementation(openrndr.application)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
                implementation(sharedLibs.kotlin.serialization.core)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-fcurve"))
                implementation(project(":orx-noise"))
            }
        }
    }
}
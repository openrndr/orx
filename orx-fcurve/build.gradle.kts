plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-expression-evaluator"))
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.filter)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.serialization.core)
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
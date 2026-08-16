@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(sharedLibs.kotest.assertions)
                implementation(sharedLibs.kotest.framework.engine)
                implementation(sharedLibs.kotlin.test)
            }
        }
        getByName("jvmMain") {
            dependencies {
                implementation(openrndr.application.core)
                implementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.10")
                implementation(openrndr.application.glfw)
                implementation(sharedLibs.bundles.jupiter)
                implementation(sharedLibs.kotlin.test)
            }
        }

    }
}
plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.math)
            implementation(sharedLibs.kotlin.reflect)
            implementation(sharedLibs.kotlin.serialization.core)
        }

        jvmMain.dependencies {
            implementation(libs.gson)
        }

        jvmTest.dependencies {
            implementation(sharedLibs.kotest.assertions)
            implementation(sharedLibs.kotest.framework.engine)
            implementation(sharedLibs.kotlin.serialization.json)
            runtimeOnly(sharedLibs.kotlin.reflect)
        }
    }
}

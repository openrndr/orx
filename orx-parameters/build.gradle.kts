plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.math)
            implementation(sharedLibs.kotlin.reflect)
        }

        jvmTest.dependencies {
            implementation(sharedLibs.kotest.assertions)
            implementation(sharedLibs.kotest.framework.engine)
            implementation(sharedLibs.kotlin.serialization.json)
            runtimeOnly(sharedLibs.kotlin.reflect)
        }
    }
}
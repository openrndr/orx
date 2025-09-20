@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(sharedLibs.kotlin.serialization.core)
                implementation(openrndr.application.core)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(sharedLibs.kotlin.serialization.json)
                implementation(sharedLibs.kotest.assertions)
                implementation(sharedLibs.kotest.framework.engine)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(sharedLibs.kotlin.serialization.json)
                implementation(sharedLibs.kotest.assertions)
                implementation(sharedLibs.kotest.framework.engine)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-color"))
                implementation(project(":orx-jvm:orx-gui"))
                implementation(project(":orx-shade-styles"))
                implementation(project(":orx-image-fit"))
                implementation(project(":orx-shapes"))
            }
        }
    }
}
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(sharedLibs.kotlin.serialization.core)
            implementation(openrndr.math)
        }

        commonTest.dependencies {
//            implementation(sharedLibs.kotlin.serialization.json)
//            implementation(sharedLibs.kotest.assertions)
//            implementation(sharedLibs.kotest.framework.engine)
        }

        jvmTest.dependencies {
//            implementation(sharedLibs.kotlin.serialization.json)
//            implementation(sharedLibs.kotest.assertions)
//            implementation(sharedLibs.kotest.framework.engine)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-camera"))
            implementation(project(":orx-mesh-generators"))
            implementation(project(":orx-color"))
            implementation(project(":orx-jvm:orx-gui"))
            implementation(project(":orx-shade-styles"))
            implementation(project(":orx-shapes"))
            implementation(project(":orx-shader-phrases"))
            implementation(project(":orx-image-fit"))
            implementation(openrndr.ffmpeg)
        }
    }
}
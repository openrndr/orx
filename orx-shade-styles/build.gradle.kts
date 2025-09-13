plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(project(":orx-color"))
                implementation(openrndr.application)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-color"))
                implementation(project(":orx-shade-styles"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-shapes"))
                implementation(project(":orx-image-fit"))
                implementation(project(":orx-camera"))
                implementation(project(":orx-obj-loader"))
            }
        }
    }
}
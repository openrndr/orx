plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        getByName("commonMain") {
            dependencies {
                implementation(openrndr.application.core)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.coroutines)
                implementation(project(":orx-bvh"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        getByName("jvmDemo") {
            dependencies {
                implementation(project(":orx-svg"))
                implementation(project(":orx-shapes"))
                implementation(project(":orx-color"))
                implementation(project(":orx-noise"))
            }
        }
    }
}

plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(libs.openrndr.application)
                api(libs.openrndr.math)
                implementation(project(":orx-shapes"))
                api(project(":orx-mesh"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-shapes"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-camera"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-obj-loader"))
            }
        }
    }
}

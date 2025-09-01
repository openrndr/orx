plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(libs.openrndr.math)
                api(libs.openrndr.shape)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-color"))
                implementation(project(":orx-shapes"))
                implementation(project(":orx-noise"))
            }
        }
    }
}
plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(libs.gson)
                implementation(project(":orx-noise"))
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-palette"))
                implementation(project(":orx-palette"))
                implementation(project(":orx-shapes"))
            }
        }
    }
}

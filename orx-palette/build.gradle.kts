plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(openrndr.application)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project(":orx-noise"))
                implementation(libs.gson)
                implementation(openrndr.math)
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

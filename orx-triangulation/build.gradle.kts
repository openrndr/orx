plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(openrndr.math)
                api(openrndr.shape)
                implementation(project(":orx-noise"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(project(":orx-shapes"))
                implementation(openrndr.shape)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-shapes"))
                implementation(project(":orx-noise"))
                implementation(openrndr.shape)
            }
        }
    }
}
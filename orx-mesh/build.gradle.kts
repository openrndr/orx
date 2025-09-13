plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(openrndr.application)
                api(openrndr.math)
                api(openrndr.shape)
                implementation(project(":orx-shapes"))
            }
        }

        val jvmDemo by getting {
            dependencies {
                api(openrndr.shape)
                implementation(project(":orx-shapes"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-obj-loader"))
                implementation(project(":orx-camera"))
                implementation(project(":orx-noise"))
            }
        }
    }
}

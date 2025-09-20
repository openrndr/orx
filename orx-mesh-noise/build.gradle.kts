plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(openrndr.application.core)
                api(openrndr.math)
                api(openrndr.shape)
                api(project(":orx-mesh"))
                implementation(project(":orx-noise"))
            }
        }

        val jvmDemo by getting {
            dependencies {
                api(openrndr.shape)
                implementation(project(":orx-shapes"))
                implementation(project(":orx-mesh"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-obj-loader"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-camera"))
            }
        }
    }
}

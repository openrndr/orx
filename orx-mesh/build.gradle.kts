plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(openrndr.application.core)
            api(openrndr.math)
            api(openrndr.shape)
            implementation(project(":orx-shapes"))
        }

        jvmDemo.dependencies {
            api(openrndr.shape)
            implementation(project(":orx-shapes"))
            implementation(project(":orx-mesh-generators"))
            implementation(project(":orx-obj-loader"))
            implementation(project(":orx-camera"))
            implementation(project(":orx-noise"))
        }
    }
}

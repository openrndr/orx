plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(openrndr.application.core)
            api(openrndr.math)
            implementation(project(":orx-shapes"))
            api(project(":orx-mesh"))
        }

        jvmDemo.dependencies {
            implementation(project(":orx-shapes"))
            implementation(project(":orx-mesh-generators"))
            implementation(project(":orx-camera"))
            implementation(project(":orx-noise"))
            implementation(project(":orx-obj-loader"))
        }
    }
}

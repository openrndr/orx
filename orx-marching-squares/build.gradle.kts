plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(openrndr.math)
            api(openrndr.shape)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-color"))
            implementation(project(":orx-shapes"))
            implementation(project(":orx-noise"))
        }
    }
}
plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
        }
        jvmMain.dependencies {
            implementation(project(":orx-noise"))
            implementation(libs.gson)
            implementation(openrndr.math)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-palette"))
            implementation(project(":orx-palette"))
            implementation(project(":orx-shapes"))
        }
    }
}

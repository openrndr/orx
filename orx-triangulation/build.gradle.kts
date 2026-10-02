plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(openrndr.math)
            api(openrndr.shape)
            implementation(project(":orx-noise"))
        }
        commonTest.dependencies {
            implementation(project(":orx-shapes"))
            implementation(openrndr.shape)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-shapes"))
            implementation(project(":orx-noise"))
            implementation(openrndr.shape)
            implementation(project(":orx-jvm:orx-remote-control"))
        }
    }
}
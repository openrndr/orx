plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.math)
            api(project(":orx-mesh"))
        }

        jvmDemo.dependencies {
            implementation(project(":orx-camera"))
            implementation(project(":orx-mesh-generators"))
        }
    }
}
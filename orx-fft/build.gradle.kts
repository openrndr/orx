plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies { }
        jvmDemo.dependencies {
            implementation(project(":orx-shapes"))
            implementation(project(":orx-noise"))
        }
    }
}
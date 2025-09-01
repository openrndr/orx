plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                api(project(":orx-mesh"))
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-mesh-generators"))
            }
        }
    }
}
plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(project(":orx-color"))
                implementation(project(":orx-kdtree"))
                implementation(openrndr.application.core)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
                implementation(sharedLibs.kotlin.coroutines)
                implementation(project(":orx-triangulation"))
            }
        }


        val commonTest by getting {
            dependencies {
                implementation(project(":orx-noise"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(sharedLibs.kotest.assertions)
                implementation(sharedLibs.kotest.framework.engine)
                implementation(sharedLibs.kotlin.serialization.json)
                runtimeOnly(sharedLibs.kotlin.reflect)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-color"))
                implementation(project(":orx-triangulation"))
                implementation(project(":orx-shapes"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-fx"))
            }
        }
    }
}


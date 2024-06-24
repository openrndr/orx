plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
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
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.filter)
                implementation(libs.kotlin.reflect)
                implementation(libs.kotlin.coroutines)
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
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotlin.serialization.json)
                runtimeOnly(libs.kotlin.reflect)
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


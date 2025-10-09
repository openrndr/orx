plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-composition"))
                implementation(openrndr.shape)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.jsoup)
                implementation(openrndr.draw)
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
                implementation(project(":orx-svg"))
            }
        }
    }
}


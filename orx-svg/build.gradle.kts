plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    alias(libs.plugins.kotest.multiplatform)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-composition"))
                implementation(libs.openrndr.shape)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.jsoup)
                implementation(libs.openrndr.draw)
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
                implementation(project(":orx-svg"))
            }
        }
    }
}


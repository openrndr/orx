plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.antlr.kotlin.runtime)
                implementation(openrndr.application.core)
                implementation(openrndr.math)
                implementation(sharedLibs.kotlin.coroutines)
                implementation(project(":orx-property-watchers"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-expression-evaluator"))
            }
        }
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-jvm:orx-gui"))
            }
        }
    }
}

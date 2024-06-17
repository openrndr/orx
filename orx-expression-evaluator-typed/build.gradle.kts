import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.antlr.kotlin.runtime)
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                implementation(libs.kotlin.coroutines)
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
        val jvmTest by getting {
            dependencies {
                implementation(libs.kluent)
            }
        }
    }
}

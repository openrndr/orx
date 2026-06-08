import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(openrndr.application.core)
                api(openrndr.math)
                api(openrndr.shape)
                implementation(sharedLibs.kotlin.serialization.core)
                implementation(sharedLibs.kotlin.serialization.json)
                implementation(sharedLibs.kotlin.serialization.cbor)
                implementation(project(":orx-rtree"))
                implementation(project(":orx-shapes"))
                implementation(project(":orx-math"))

            }
        }

        val jvmDemo by getting {
            dependencies {
                api(openrndr.shape)
                implementation(project(":orx-shapes"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-obj-loader"))
                implementation(project(":orx-camera"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-color"))
                implementation(sharedLibs.kotlin.coroutines)
                implementation(openrndr.ffmpeg)
            }
        }
    }
}

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

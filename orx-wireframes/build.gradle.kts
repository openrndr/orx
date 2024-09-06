plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.wireframes")
    defaultVisibility.set("internal")
    namePrefix.set("wireframes_")
}.get()

kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                implementation(project(":orx-compute-shaders"))
            }
        }
        val commonTest by getting {
            dependencies {
                api(libs.kotest.assertions)
            }
        }
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-point-clouds"))
            }
        }
    }
}

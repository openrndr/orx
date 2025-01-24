plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    alias(libs.plugins.kotest.multiplatform)
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.noise.filters")
    defaultVisibility.set("internal")
    namePrefix.set("noise_")
}.get()

kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.math)
                implementation(libs.openrndr.shape)
                implementation(libs.openrndr.draw)
                implementation(project(":orx-hash-grid"))
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(libs.kotest.framework.engine)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-hash-grid"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-jvm:orx-gui"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-camera"))
            }
        }
    }
}
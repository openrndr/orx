plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.meshgenerators")
    defaultVisibility.set("internal")
    namePrefix.set("meshgenerators_")
}.get()

kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.openrndr.application)
                api(libs.openrndr.math)
                implementation(project(":orx-shapes"))
                implementation(project(":orx-compute-shaders"))
            }
        }
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-shapes"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-camera"))
                implementation(project(":orx-noise"))
                implementation(project(":orx-point-clouds"))
            }
        }
    }
}

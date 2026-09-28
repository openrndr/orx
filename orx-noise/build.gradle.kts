plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    description = "Embeds .glsl files as strings in .kt files with matching file names"
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.noise.filters")
    defaultVisibility.set("internal")
    namePrefix.set("noise_")
}.get()

kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.math)
            implementation(openrndr.shape)
            implementation(openrndr.draw)
            implementation(project(":orx-hash-grid"))
            implementation(project(":orx-parameters"))
            implementation(project(":orx-shader-phrases"))
            api(project(":orx-math"))
        }

        jvmTest.dependencies {
            implementation(sharedLibs.kotest.assertions)
            implementation(sharedLibs.kotest.framework.engine)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-hash-grid"))
            implementation(project(":orx-noise"))
            implementation(project(":orx-jvm:orx-gui"))
            implementation(project(":orx-mesh-generators"))
            implementation(project(":orx-camera"))
        }
    }
}
plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    description = "Embeds .glsl files as strings in .kt files with matching file names"
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.fx")
    defaultVisibility.set("internal")
    namePrefix.set("fx_")
}.get()


kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":orx-parameters"))
            implementation(project(":orx-shader-phrases"))
            implementation(project(":orx-color"))
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
            implementation(openrndr.filter)
            implementation(sharedLibs.kotlin.reflect)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-color"))
            implementation(project(":orx-fx"))
            implementation(project(":orx-noise"))
            implementation(project(":orx-shapes"))
            implementation(project(":orx-image-fit"))
        }
    }
}
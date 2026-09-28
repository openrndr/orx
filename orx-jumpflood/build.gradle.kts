plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    description = "Embeds .glsl files as strings in .kt files with matching file names"
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.jumpflood")
    defaultVisibility.set("internal")
    namePrefix.set("jf_")
}.get()

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(embedShaders.outputDir)
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-fx"))
                implementation(openrndr.application.core)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
            }
        }

        jvmDemo.dependencies {
            implementation(project(":orx-color"))
            implementation(project(":orx-fx"))
            implementation(project(":orx-noise"))
            implementation(project(":orx-jumpflood"))
            implementation(project(":orx-compositor"))
            implementation(project(":orx-jvm:orx-gui"))
            implementation(project(":orx-composition"))
            implementation(project(":orx-svg"))
        }
    }
}
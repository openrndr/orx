plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.extra.jumpflood")
    defaultVisibility.set("internal")
    namePrefix.set("jf_")
}.get()

kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-fx"))
                implementation(openrndr.application)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
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
}
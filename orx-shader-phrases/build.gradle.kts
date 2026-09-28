plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(layout.buildDirectory.dir("generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.shaderphrases.phrases")
}.get()

kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
            implementation(sharedLibs.kotlin.reflect)
        }

        jvmTest.dependencies {
            runtimeOnly(sharedLibs.slf4j.simple)
            runtimeOnly(sharedLibs.kotlin.reflect)
            implementation(sharedLibs.kotest.assertions)
            implementation(sharedLibs.kotest.framework.engine)
        }
    }
}
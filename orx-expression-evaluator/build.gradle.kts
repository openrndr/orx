import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    alias(libs.plugins.antlr.kotlin)
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    source = fileTree(layout.projectDirectory.dir("src/commonMain/antlr")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "org.openrndr.extra.expressions.parser"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}


kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.antlr.kotlin.runtime)
                implementation(openrndr.application.core)
                implementation(openrndr.math)
                implementation(sharedLibs.kotlin.coroutines)
                implementation(project(":orx-property-watchers"))
                implementation(project(":orx-noise"))
            }
            kotlin {
                srcDir(layout.buildDirectory.dir("generatedAntlr"))
            }
        }
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-jvm:orx-gui"))
            }
        }
    }
}

tasks.withType<KotlinCompilationTask<*>> { dependsOn(generateKotlinGrammarSource) }
tasks.withType<org.gradle.jvm.tasks.Jar> { dependsOn(generateKotlinGrammarSource) }
tasks.named("dokkaGeneratePublicationHtml") { dependsOn(generateKotlinGrammarSource) }
tasks.named("dokkaGenerateModuleHtml") { dependsOn(generateKotlinGrammarSource) }

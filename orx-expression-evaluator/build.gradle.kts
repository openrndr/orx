import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    alias(libs.plugins.antlr.kotlin)
}

val generateKotlinGrammarSource = tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    dependsOn("cleanGenerateKotlinGrammarSource")

    // ANTLR .g4 files are under {example-project}/antlr
    // Only include *.g4 files. This allows tools (e.g., IDE plugins)
    // to generate temporary files inside the base path
    source = fileTree(layout.projectDirectory.dir("src/commonMain/antlr")) {
        include("**/*.g4")
    }

    // We want the generated source files to have this package name
    val pkgName = "org.openrndr.expressions.parser"
    packageName = pkgName

    // We want visitors alongside listeners.
    // The Kotlin target language is implicit, as is the file encoding (UTF-8)
    arguments = listOf("-visitor")

    // Generated files are outputted inside build/generatedAntlr/{package-name}
    val outDir = "generatedAntlr/${pkgName.replace(".", "/")}"
    outputDirectory = layout.buildDirectory.dir(outDir).get().asFile
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.antlr.kotlin.runtime)
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.math)
                implementation(libs.kotlin.coroutines)
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
        val jvmTest by getting {
            dependencies {
                implementation(libs.kluent)
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    dependsOn(generateKotlinGrammarSource)
}

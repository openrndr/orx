package org.openrndr.extra.convention

import CollectScreenshotsTask
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.nativeplatform.platform.internal.DefaultOperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val libs = the<LibrariesForLibs>()

val shouldPublish = project.name !in setOf("openrndr-demos")

plugins {
    kotlin("multiplatform")
    `maven-publish` apply false
    id("org.openrndr.extra.convention.component-metadata-rule")
    id("org.openrndr.extra.convention.dokka")
    signing
}
if (shouldPublish) {
    apply(plugin = "maven-publish")
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "org.openrndr.extra"

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        apiVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.versions.kotlinApi.get().replace(".", "_")}"))
        languageVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.versions.kotlinLanguage.get().replace(".", "_")}"))
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
        freeCompilerArgs.add("-Xjdk-release=${libs.versions.jvmTarget.get()}")
    }
}

kotlin {
    jvm {
        compilations {
            val main by getting

            val demo by creating {
                associateWith(main)
                tasks.register<CollectScreenshotsTask>("collectScreenshots") {
                    // since Kotlin 2.1.20 output.classesDirs no longer contains a single file
                    inputDir.set(output.classesDirs.filter { it.path.contains("classes/kotlin") }.singleFile)
                    runtimeDependencies.set(runtimeDependencyFiles)
                    outputDir.set(project.file(project.projectDir.toString() + "/images"))
                    dependsOn(compileTaskProvider)
                }
                dependencies {
                    if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
                        runtimeOnly(libs.openrndr.gl3.natives.macos.arm64)
                    }
                }
            }
        }
        testRuns["test"].executionTask {
            useJUnitPlatform()
            testLogging.exceptionFormat = TestExceptionFormat.FULL
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            classpath(kotlin.jvm().compilations.getByName("demo").output.allOutputs)
            classpath(kotlin.jvm().compilations.getByName("demo").configurations.runtimeDependencyConfiguration!!)
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.logging)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.bundles.jupiter)
                runtimeOnly(libs.slf4j.simple)
            }
        }

        val jvmDemo by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.extensions)
                runtimeOnly(libs.openrndr.gl3.core)
                runtimeOnly(libs.slf4j.simple)
            }
        }
    }
}

val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")

if (shouldPublish) {
    publishing {
        publications {
            val fjdj = tasks.register("fakeJavaDocJar", Jar::class) {
                archiveClassifier.set("javadoc")
            }
            named("js") {
                this as MavenPublication
                versionMapping {
                    allVariants {
                        fromResolutionOf("jsMainResolvableDependenciesMetadata")
                    }
                }
            }
            named("jvm") {
                this as MavenPublication
                this.artifact(fjdj)
                versionMapping {
                    allVariants {
                        fromResolutionOf("jvmMainResolvableDependenciesMetadata")
                    }
                }
            }
            named("kotlinMultiplatform") {
                this as MavenPublication
                versionMapping {
                    allVariants {
                        fromResolutionOf("commonMainResolvableDependenciesMetadata")
                    }
                }
            }
            all {
                this as MavenPublication
                pom {
                    name.set(project.name)
                    description.set(project.name)
                    url.set("https://openrndr.org")
                    developers {
                        developer {
                            id.set("edwinjakobs")
                            name.set("Edwin Jakobs")
                            email.set("edwin@openrndr.org")
                        }
                    }

                    licenses {
                        license {
                            name.set("BSD-2-Clause")
                            url.set("https://github.com/openrndr/orx/blob/master/LICENSE")
                            distribution.set("repo")
                        }
                    }

                    scm {
                        connection.set("scm:git:git@github.com:openrndr/orx.git")
                        developerConnection.set("scm:git:ssh://github.com/openrndr/orx.git")
                        url.set("https://github.com/openrndr/orx")
                    }
                }
            }
        }
    }

    signing {
        setRequired({ isReleaseVersion && gradle.taskGraph.hasTask("publish") })
        sign(publishing.publications)
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach {
    workingDir = rootDir
    val os: OperatingSystem? = DefaultNativePlatform.getCurrentOperatingSystem()
    if (os?.name == "Mac OS X") {
        setJvmArgs(listOf("-XstartOnFirstThread"))
    }
}

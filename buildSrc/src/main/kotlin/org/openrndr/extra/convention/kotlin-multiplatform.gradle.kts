package org.openrndr.extra.convention

import CollectScreenshotsTask
import gradle.kotlin.dsl.accessors._a37e1a3c5785f18372ed85a4dc9bbdf6.testRuntimeOnly
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

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

tasks.withType<KotlinCompile>() {
    kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
    kotlinOptions.languageVersion = libs.versions.kotlinLanguage.get()
}

kotlin {
    jvm {
        jvmToolchain(libs.versions.jvmTarget.get().toInt())
        compilations {
            val main by getting

            @Suppress("UNUSED_VARIABLE")
            val demo by creating {
                associateWith(main)
                tasks.register<CollectScreenshotsTask>("collectScreenshots") {
                    inputDir.set(output.classesDirs.singleFile)
                    runtimeDependencies.set(runtimeDependencyFiles)
                    outputDir.set(project.file(project.projectDir.toString() + "/images"))
                    dependsOn(compileTaskProvider)
                }
            }
        }
        testRuns["test"].executionTask {
            useJUnitPlatform()
            testLogging.exceptionFormat = TestExceptionFormat.FULL
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.logging)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.bundles.jupiter)
                runtimeOnly(libs.slf4j.simple)
            }
        }

        @Suppress("UNUSED_VARIABLE")
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
            val fjdj = tasks.create("fakeJavaDocJar", Jar::class) {
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

kotlin {
    jvm().mainRun {
        classpath(kotlin.jvm().compilations.getByName("demo").output.allOutputs)
        classpath(kotlin.jvm().compilations.getByName("demo").configurations.runtimeDependencyConfiguration!!)
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach { workingDir = rootDir }
package org.openrndr.extra.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.openrndr.extra.convention.component-metadata-rule")
    id("org.openrndr.extra.convention.dokka")
}

repositories {
    mavenCentral()
    maven {
        // This is needed to resolve `com.github.ricardomatias:delaunator`
        url = URI("https://maven.openrndr.org")
    }
    mavenLocal()
}

group = "org.openrndr.extra"

tasks.withType<KotlinCompile>() {
    kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
    kotlinOptions.languageVersion = libs.versions.kotlinLanguage.get()
}

kotlin {
    jvm {
        jvmToolchain {
            this as JavaToolchainSpec
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
        }
        compilations {
            val main by getting
            @Suppress("UNUSED_VARIABLE")
            val demo by creating {
                defaultSourceSet {
                    dependencies {
                        implementation(main.output.allOutputs)
                    }
                }
            }
        }
        testRuns["test"].executionTask {
            useJUnitPlatform()
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
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.extensions)
                runtimeOnly(libs.openrndr.gl3.core)
            }
        }
    }
}
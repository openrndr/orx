package org.openrndr.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.openrndr.convention.component-metadata-rule")
    id("org.openrndr.convention.dokka")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

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
                dependencies {
                    implementation(main.compileDependencyFiles + main.output.classesDirs)
                }
            }
        }
        testRuns["test"].executionTask.configure {
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
        val jvmMain by getting

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                runtimeOnly(libs.bundles.jupiter)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependsOn(jvmMain)
        }
    }
}
package org.openrndr.extra.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val libs = the<LibrariesForLibs>()

plugins {
    java
    kotlin("jvm")
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

val main by sourceSets.getting

@Suppress("UNUSED_VARIABLE")
val demo by sourceSets.creating

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.logging)
    testImplementation(libs.kotlin.test)
    "demoImplementation"(main.output.classesDirs + main.runtimeClasspath)
    "demoImplementation"(libs.openrndr.application)
    "demoImplementation"(libs.openrndr.extensions)
    "demoRuntimeOnly"(libs.openrndr.gl3.core)
}

kotlin {
    jvmToolchain {
        this as JavaToolchainSpec
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
    }
}

tasks {
    @Suppress("UNUSED_VARIABLE")
    val test by getting(Test::class) {
        useJUnitPlatform()
    }

    @Suppress("UNUSED_VARIABLE")
    val javadoc by getting(Javadoc::class) {
        options {
            this as StandardJavadocDocletOptions
            addBooleanOption("Xdoclint:none", true)
        }
    }
    withType<KotlinCompile>() {
        kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
        kotlinOptions.languageVersion = libs.versions.kotlinLanguage.get()
    }
}
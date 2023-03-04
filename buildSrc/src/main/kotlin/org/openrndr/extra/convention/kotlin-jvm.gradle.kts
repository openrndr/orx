package org.openrndr.extra.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    java
    kotlin("jvm")
    id("maven-publish")
    id("org.openrndr.extra.convention.component-metadata-rule")
    id("org.openrndr.extra.convention.dokka")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "org.openrndr.extra"

val main: SourceSet by sourceSets.getting

@Suppress("UNUSED_VARIABLE")
val demo: SourceSet by sourceSets.creating

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.logging)
    testImplementation(libs.kotlin.test)
    "demoImplementation"(main.output.classesDirs + main.runtimeClasspath)
    "demoImplementation"(libs.openrndr.application)
    "demoImplementation"(libs.openrndr.extensions)
    "demoRuntimeOnly"(libs.openrndr.gl3.core)
    "demoRuntimeOnly"(libs.slf4j.simple)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

tasks {
    @Suppress("UNUSED_VARIABLE")
    val test by getting(Test::class) {
        useJUnitPlatform()
        testLogging.exceptionFormat = TestExceptionFormat.FULL
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
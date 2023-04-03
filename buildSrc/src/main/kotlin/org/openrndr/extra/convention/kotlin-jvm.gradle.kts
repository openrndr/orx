package org.openrndr.extra.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val libs = the<LibrariesForLibs>()

val shouldPublish = project.name !in setOf("openrndr-demos")

plugins {
    java
    kotlin("jvm")
    `maven-publish` apply false
    id("org.openrndr.extra.convention.component-metadata-rule")
    id("org.openrndr.extra.convention.dokka")
    signing
}
if (shouldPublish) {
    apply(plugin = "maven-publish")
}

//apply(plugin = "nebula.release")

repositories {
    mavenCentral()
    mavenLocal()
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

java {
    withJavadocJar()
    withSourcesJar()
}

val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")

if (shouldPublish) {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                groupId = "org.openrndr.extra"
                artifactId = project.name
                description = project.name
                versionMapping {
                    allVariants {
                        fromResolutionResult()
                    }
                }
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
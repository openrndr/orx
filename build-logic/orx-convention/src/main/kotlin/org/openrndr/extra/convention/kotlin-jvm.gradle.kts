package org.openrndr.extra.convention

import ScreenshotsHelper.collectScreenshots
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val sharedLibs = extensions.getByType(VersionCatalogsExtension::class.java).named("sharedLibs")
val openrndr = extensions.getByType(VersionCatalogsExtension::class.java).named("openrndr")
val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

val shouldPublish = project.name !in setOf("openrndr-demos", "orx-git-archiver-gradle")

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

repositories {
    mavenCentral()
    mavenLocal()
}

group = "org.openrndr.extra"

val main: SourceSet by project.sourceSets.getting

@Suppress("UNUSED_VARIABLE")
val demo: SourceSet by project.sourceSets.creating {
    val skipDemos = setOf(
        "openrndr-demos",
        "orx-axidraw",
        "orx-midi",
        "orx-minim",
        "orx-realsense2",
        "orx-runway",
        "orx-syphon",
        "orx-video-profiles",
        "orx-crash-handler"
    )
    if (project.name !in skipDemos) {
        collectScreenshots(project, this@creating) { }
    }
}

dependencies {
    implementation(sharedLibs.findLibrary("kotlin-stdlib").get())
    implementation(sharedLibs.findLibrary("kotlin-logging").get())
    testImplementation(sharedLibs.findLibrary("kotlin-test").get())
    testRuntimeOnly(sharedLibs.findLibrary("slf4j-simple").get())
    "demoImplementation"(main.output.classesDirs + main.runtimeClasspath)
    "demoImplementation"(openrndr.findLibrary("application-core").get())
    "demoImplementation"(openrndr.findLibrary("orextensions").get())

    "demoRuntimeOnly"(openrndr.findLibrary("application-glfw").get())

    "demoRuntimeOnly"(sharedLibs.findLibrary("slf4j-simple").get())
}

tasks {
    @Suppress("UNUSED_VARIABLE")
    val test by getting(Test::class) {
        if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
            allJvmArgs = allJvmArgs + "-XstartOnFirstThread"
        }
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
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.valueOf("JVM_${libs.findVersion("jvmTarget").get().displayName.replace(".", "_")}"))
            freeCompilerArgs.add("-Xexpect-actual-classes")
            freeCompilerArgs.add("-Xjdk-release=${libs.findVersion("jvmTarget").get().displayName}")
            apiVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.findVersion("kotlinApi").get().displayName.replace(".", "_")}"))
            languageVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.findVersion("kotlinLanguage").get().displayName.replace(".", "_")}"))
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
    targetCompatibility = JavaVersion.valueOf("VERSION_${libs.findVersion("jvmTarget").get().displayName}")
    sourceCompatibility = JavaVersion.valueOf("VERSION_${libs.findVersion("jvmTarget").get().displayName}")
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

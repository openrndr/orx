import Orx_collect_screenshots_gradle.ScreenshotsHelper.collectScreenshots
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("orx.collect-screenshots")
}

val kotlinxSerializationVersion: String by rootProject.extra
val kotestVersion: String by rootProject.extra
val junitJupiterVersion: String by rootProject.extra
val jvmTarget: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra
val kluentVersion: String by rootProject.extra
val openrndrVersion: String by rootProject.extra
val openrndrOS: String by rootProject.extra
val spekVersion: String by rootProject.extra

kotlin {
    jvm {
        compilations {
            val demo by creating {
                defaultSourceSet {
                    kotlin.srcDir("src/demo")
                    dependencies {
                        implementation(project(":orx-camera"))
                        implementation(project(":orx-color"))
                        implementation("org.openrndr:openrndr-application:$openrndrVersion")
                        implementation("org.openrndr:openrndr-extensions:$openrndrVersion")
                        runtimeOnly("org.openrndr:openrndr-gl3:$openrndrVersion")
                        runtimeOnly("org.openrndr:openrndr-gl3-natives-$openrndrOS:$openrndrVersion")
                        implementation(compilations["main"]!!.output.allOutputs)
                    }
                }
                collectScreenshots {

                }
            }
        }
        compilations.all {
            kotlinOptions.jvmTarget = jvmTarget
            kotlinOptions.apiVersion = kotlinApiVersion
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
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(project(":orx-color"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
                implementation("org.openrndr:openrndr-application:$openrndrVersion")
                implementation("org.openrndr:openrndr-draw:$openrndrVersion")
                implementation("org.openrndr:openrndr-filter:$openrndrVersion")
                implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }
        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
                implementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
                implementation("org.amshove.kluent:kluent:$kluentVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}
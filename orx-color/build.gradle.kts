import ScreenshotsHelper.collectScreenshots

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
    // kotlinx-serialization ends up on the classpath through openrndr-math and Gradle doesn't know which
    // version was used. If openrndr were an included build, we probably wouldn't need to do this.
    // https://github.com/gradle/gradle/issues/20084
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

kotlin {
    jvm {
        @Suppress("UNUSED_VARIABLE")
        val demo by compilations.getting {
            collectScreenshots { }
        }
        testRuns["test"].executionTask {
            useJUnitPlatform {
                includeEngines("spek2")
            }
        }
    }

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":orx-parameters"))
                implementation(project(":orx-shader-phrases"))
                implementation(libs.kotlin.serialization.core)
                implementation(libs.openrndr.application)
                implementation(libs.openrndr.draw)
                implementation(libs.openrndr.filter)
                implementation(libs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.serialization.json)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(libs.kluent)
                implementation(libs.spek.dsl)
                runtimeOnly(libs.spek.junit5)
                runtimeOnly(libs.kotlin.reflect)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-camera"))
                implementation(project(":orx-mesh-generators"))
                implementation(project(":orx-color"))
                implementation(project(":orx-jvm:orx-gui"))
            }
        }
    }
}
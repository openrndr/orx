plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
    // kotlinx-serialization ends up on the classpath through openrndr-math and Gradle doesn't know which
    // version was used. If openrndr were an included build, we probably wouldn't need to do this.
    // https://github.com/gradle/gradle/issues/20084
    id(libs.plugins.kotlin.serialization.get().pluginId)
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(openrndr.application)
                implementation(openrndr.draw)
                implementation(openrndr.filter)
                implementation(sharedLibs.kotlin.reflect)
                implementation(sharedLibs.kotlin.serialization.core)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmDemo by getting {
            dependencies {
                implementation(project(":orx-svg"))
            }
        }
    }
}

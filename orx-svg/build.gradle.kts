plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":orx-composition"))
            implementation(openrndr.shape)
        }

        jvmMain.dependencies {
            implementation(libs.jsoup)
            implementation(openrndr.draw)
        }

        jvmTest.dependencies {
            implementation(sharedLibs.kotest.assertions)
            implementation(sharedLibs.kotest.framework.engine)
            implementation(sharedLibs.kotlin.serialization.json)
            runtimeOnly(sharedLibs.kotlin.reflect)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-svg"))
        }
    }
}


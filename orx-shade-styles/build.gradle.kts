plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":orx-parameters"))
            implementation(project(":orx-shader-phrases"))
            implementation(project(":orx-color"))
            implementation(openrndr.application.core)
            implementation(openrndr.draw)
            implementation(openrndr.filter)
            implementation(sharedLibs.kotlin.reflect)
        }

        getByName("jvmTest") {
            dependencies {
                implementation(project(":testing"))
            }
        }

        jvmDemo.dependencies {
            implementation(project(":orx-color"))
            implementation(project(":orx-shade-styles"))
            implementation(project(":orx-noise"))
            implementation(project(":orx-shapes"))
            implementation(project(":orx-image-fit"))
            implementation(project(":orx-camera"))
            implementation(project(":orx-obj-loader"))
        }
    }
}

tasks.named<Test>("jvmTest") {
    // every test class that uses the test fixture opens a window, opening more than one window per JVM is unreliable
    forkEvery = 1
    if (org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }
}

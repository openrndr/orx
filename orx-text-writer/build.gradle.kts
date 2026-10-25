plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.shape)
            implementation(openrndr.draw)
            implementation(openrndr.application.core)
        }

        jvmDemo.dependencies {
            implementation(project(":orx-text-writer"))
            implementation(project(":orx-shapes"))
            implementation(project(":orx-camera"))
        }

        getByName("jvmTest") {
            dependencies {
                implementation(project(":testing"))
                implementation(openrndr.fontdriver.freetype)
                implementation(openrndr.textshapingdriver.harfbuzz)
                implementation(openrndr.bidi.text.driver)
            }
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

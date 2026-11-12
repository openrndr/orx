plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(openrndr.shape)
            implementation(openrndr.draw)
            implementation(openrndr.application.core)
            implementation(project(":orx-shapes"))
            api(project(":orx-text-writer"))
            // the extrusion of text writes into a TriangleMeshBuilder
            api(project(":orx-mesh-generators"))
        }

        jvmDemo.dependencies {
            implementation(openrndr.ffmpeg)
            implementation(project(":orx-text-on-contour"))
            implementation(project(":orx-camera"))
        }

        // the tests of the extrusion of text shape glyphs, which takes the font and shaping drivers
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
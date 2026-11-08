plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(openrndr.color)
            api(openrndr.math)
            // the companion of Linearity of openrndr-color is a serializer factory, which the compilers of js and wasm resolve
            implementation(sharedLibs.kotlin.serialization.core)
        }
    }
}

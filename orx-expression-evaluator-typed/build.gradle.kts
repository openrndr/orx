plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.antlr.kotlin.runtime)
            implementation(openrndr.application.core)
            implementation(openrndr.math)
            implementation(sharedLibs.kotlin.coroutines)
            implementation(project(":orx-property-watchers"))
            implementation(project(":orx-noise"))
            implementation(project(":orx-expression-evaluator"))
        }
        jvmDemo.dependencies {
            implementation(project(":orx-jvm:orx-gui"))
        }
    }
}

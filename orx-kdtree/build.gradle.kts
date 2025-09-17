plugins {
    id("org.openrndr.extra.convention.kotlin-multiplatform")
}


kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(openrndr.application)
                api(openrndr.math)
                implementation(sharedLibs.kotlin.coroutines)
                api(openrndr.utils)
            }
        }
    }
}



//tasks.withType<KotlinCompile> {
//    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
//}

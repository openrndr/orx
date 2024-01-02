plugins {
    org.openrndr.extra.convention.`kotlin-multiplatform`
}


kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.openrndr.application)
                api(libs.openrndr.math)
                implementation(libs.kotlin.coroutines)
                api(libs.openrndr.utils)
            }
        }
    }
}



//tasks.withType<KotlinCompile> {
//    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
//}

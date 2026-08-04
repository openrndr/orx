plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
}

dependencies {
    api(libs.processing.core) {
        exclude(group = "org.jogamp.gluegen")
        exclude(group = "org.jogamp.jogl")
    }
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.reflect)
    demoRuntimeOnly(sharedLibs.slf4j.simple)
    demoImplementation(project(":orx-shapes"))
}

//// Processing Geometry Suite - https://github.com/micycle1/PGS
//// Add the following repositories and dependencies to your openrndr-template
//// to experiment with PGS in OPENRNDR. Uncomment them below to run
//// DemoPGS01.kt or other PGS demos.
//
//repositories {
//    maven { url = uri("https://jitpack.io") }
//    maven(url = "https://jogamp.org/deployment/maven/")
//    maven(url = "https://ojrepo.soldin.de/")
//}
//dependencies {
//    implementation("com.github.openjump-gis:OpenJUMP:2.4.0") {
//        exclude("javax.media")
//        exclude("it.geosolutions.imageio-ext")
//    }
//    implementation("com.github.micycle1:JMedialAxis:5207bec2f2")
//    implementation("com.github.edwinRNDR:PGS:-SNAPSHOT") {
//        exclude(group = "quil")
//        exclude(group = "org.openjump")
//        exclude(group = "org.jogamp.jogl")
//        exclude(group = "org.jogamp.gluegen")
//    }
//}
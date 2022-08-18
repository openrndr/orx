plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
}

dependencies {
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.gson)
    implementation(libs.jarchivelib)
    implementation(project(":orx-noise"))
    testImplementation(libs.kluent)
    testImplementation(libs.spek.dsl)
    testRuntimeOnly(libs.spek.junit5)
}

tasks.test {
    useJUnitPlatform {
        includeEngines("spek2")
    }
}
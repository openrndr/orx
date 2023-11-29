import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    org.openrndr.extra.convention.`kotlin-jvm`
    antlr
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments.addAll(listOf("-visitor", "-long-messages"))
}


tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
}

dependencies {
    antlr(libs.antlr.core)
    implementation(libs.antlr.runtime)
    implementation(libs.openrndr.application)
    implementation(libs.openrndr.math)
    implementation(libs.kotlin.coroutines)
    implementation(project(":orx-property-watchers"))
    implementation(project(":orx-noise"))
    testImplementation(libs.kluent)
    demoImplementation(project(":orx-jvm:orx-gui"))
}

tasks.getByName("compileKotlin").dependsOn("generateGrammarSource")
tasks.getByName("compileDemoKotlin").dependsOn("generateDemoGrammarSource")
tasks.getByName("compileTestKotlin").dependsOn("generateTestGrammarSource")
tasks.getByName("sourcesJar").dependsOn("generateGrammarSource")

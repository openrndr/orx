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
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    antlr(libs.antlr.core)
    implementation(libs.antlr.runtime)
    testImplementation(libs.kluent)
}

tasks.getByName("compileKotlin").dependsOn("generateGrammarSource")
tasks.getByName("sourcesJar").dependsOn("generateGrammarSource")

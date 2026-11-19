plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
    alias(libs.plugins.kotlin.serialization)
}

tasks.test {
    useJUnitPlatform {
    }
    // test classes that use the application test fixture open a window, opening more than one window per JVM is
    // unreliable
    forkEvery = 1
    if (org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
        jvmArgs("-XstartOnFirstThread")
    }
}

dependencies {
    // SelectableList and SelectableMutableList implement Selectable
    api(project(":orx-parameters"))
    implementation(project(":orx-expression-evaluator"))
    // for the tokens of the expressions of ExpressionArea
    implementation(libs.antlr.kotlin.runtime)
    implementation(project(":orx-color"))
    implementation(project(":orx-text-writer"))
    api(project(":orx-style"))
    implementation(project(":orx-view-box"))
    implementation(project(":orx-image-fit"))
    implementation(openrndr.application.core)
    implementation(openrndr.math)
    implementation(sharedLibs.kotlin.coroutines)
    implementation(sharedLibs.kotlin.reflect)
    // the editor of fcurves, which sculpts them with the distortions of orx-shapes
    api(project(":orx-fcurve"))
    implementation(project(":orx-shapes"))
    // the layouts of docks are saved as JSON
    implementation(sharedLibs.kotlin.serialization.json)
    testRuntimeOnly(sharedLibs.kotlin.reflect)
    // a program with fonts, keys and a clipboard for tests that lay out text and type
    testImplementation(project(":testing"))
    // the typed expressions of ExpressionArea
    testImplementation(project(":orx-expression-evaluator"))
    testImplementation(project(":orx-expression-evaluator-typed"))
    demoImplementation(project(":orx-expression-evaluator"))
    demoImplementation(project(":orx-expression-evaluator-typed"))
    testRuntimeOnly(openrndr.fontdriver.freetype)
    demoImplementation(openrndr.dialogs)
    demoImplementation(libs.gson)
    demoImplementation(project(":orx-jvm:orx-panel"))
    demoImplementation(project(":orx-shapes"))
    demoImplementation(project(":orx-camera"))
    demoImplementation(sharedLibs.kotlin.serialization.json)
}
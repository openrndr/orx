package org.openrndr.extra.convention

// Applied to the root project: registers buildMainReadme, which regenerates the orx list in README.md
tasks.register<BuildMainReadmeTask>("buildMainReadme") {
    group = "documentation"
    description = "Regenerates the orx list in the main README.md from the module READMEs."
    mainReadme.set(layout.projectDirectory.file("README.md"))
    rootDirectory.set(layout.projectDirectory)
    modulePaths.set(subprojects.map { it.projectDir.relativeTo(rootDir).invariantSeparatorsPath })
}

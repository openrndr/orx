package org.openrndr.extra.gitarchiver

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.InputChanges
import java.io.File
import javax.inject.Inject

abstract class GitArchiveToMarkdown @Inject constructor() : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputDirectory
    abstract val gitDir: DirectoryProperty

    @get:InputDirectory
    abstract val screenshotsDir: DirectoryProperty

    @get:Input
    abstract val historySize: Property<Int>

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val parent = outputDir.asFile.get()

        val git = GitProvider.create()
        val references = git.logReferences(historySize.get())

        val text = references.map { reference ->
            val screenshots = screenshotsDir.asFile.get().listFiles().filter { file ->
                file.extension == "png" && file.name.contains(reference)
            }
            println(screenshots)
            screenshots.forEach {
                it.copyTo(File(outputDir.asFile.get(), it.name))
            }
            val screenShotsMD = screenshots.map {
                "![${it.nameWithoutExtension}](${it.name})"
            }.joinToString("\n")

            """# $reference
                |$screenShotsMD
                |```
                |${git.show(reference)}}
                |```
            """.trimMargin()
        }.joinToString("\n")
        File(parent, "README.md").writeText(text)
    }

    init {
        outputDir.set(File("build/git-archive-markdown"))
        gitDir.set(File(".git"))
        screenshotsDir.set(File("screenshots"))
        historySize.set(20)
    }
}
package org.openrndr.extra.gitarchiver

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class GitArchiveToMarkdown @Inject constructor() : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val gitDir: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val screenshotsDir: DirectoryProperty

    @get:Input
    abstract val historySize: Property<Int>

    @TaskAction
    fun execute() {
        val parent = outputDir.asFile.get()

        val git = GitProvider.create()
        val references = git.logReferences(historySize.get())

        val text = references.joinToString("\n") { reference ->
            val screenshots = screenshotsDir.asFile.get().listFiles().filter { file ->
                file.extension == "png" && file.name.contains(reference)
            }
            println(screenshots)
            screenshots.forEach {
                it.copyTo(File(outputDir.asFile.get(), it.name), true)
            }
            val screenShotsMD = screenshots.joinToString("\n") {
                "![${it.nameWithoutExtension}](${it.name})"
            }

            """# $reference
                |$screenShotsMD
                |```
                |${git.show(reference)}}
                |```
            """.trimMargin()
        }
        File(parent, "README.md").writeText(text)
    }

    init {
        outputDir.set(File("build/git-archive-markdown"))
        gitDir.set(File(".git"))
        screenshotsDir.set(File("screenshots"))
        historySize.set(20)
    }
}
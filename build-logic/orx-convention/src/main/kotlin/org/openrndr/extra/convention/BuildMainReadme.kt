package org.openrndr.extra.convention

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

/**
 * Regenerates the orx list between `<!-- __orxListBegin__ -->` and `<!-- __orxListEnd__ -->` in the main README.md,
 * using the description at the top of each module's README.md.
 *
 * The main README is rewritten in place, so the task declares no outputs and always runs.
 */
abstract class BuildMainReadmeTask : DefaultTask() {
    /** The README.md to update */
    @get:Internal
    abstract val mainReadme: RegularFileProperty

    /** The directory module paths are relative to */
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    /** Module directories relative to [rootDirectory], e.g. `orx-jvm/orx-gui` */
    @get:Input
    abstract val modulePaths: ListProperty<String>

    @TaskAction
    fun execute() {
        // Load README.md and find [begin, end] section to replace
        val mainReadme = mainReadme.get().asFile
        val lines = mainReadme.readLines()

        val begin = lines.indexOfFirst { it == "<!-- __orxListBegin__ -->" }
        val end = lines.indexOfFirst { it == "<!-- __orxListEnd__ -->" }
        if (begin == -1 || end == -1) {
            logger.warn("Comments for orx list generation not found in README.md!")
            return
        }

        val header = lines.subList(0, begin + 1)
        val footer = lines.subList(end, lines.size)

        val newReadme = mutableListOf<String>()
        newReadme.addAll(header)

        // Search for the description at the top of the readme.
        // Skip the hash character from the headline, then start
        // on the next line and continue until the next empty line.
        // Don't fall into Windows line breaks.
        val descriptionRx = Regex("(?s)#.*?\\n(.+?)\\n\\r?\\n")
        // Note: the readme needs an empty line after the description

        val orxMultiplatform = mutableListOf<String>()
        val orxJVMOnly = mutableListOf<String>()

        // Build orx list, skipping modules without a README.md
        val rootDir = rootDirectory.get().asFile
        for (path in modulePaths.get()) {
            val orxReadmeFile = rootDir.resolve("$path/README.md")
            if (!orxReadmeFile.exists()) continue
            val match = descriptionRx.find(orxReadmeFile.readText()) ?: continue
            val trimmedDescription = match.groupValues[1].trim().replace("\n", " ").replace("\r", "")
            val row = "| [`${path.substringAfterLast("/")}`]($path/) | $trimmedDescription |"
            if (path.startsWith("orx-jvm")) orxJVMOnly.add(row) else orxMultiplatform.add(row)
        }

        newReadme.add("\n## Multiplatform\n")
        newReadme.add("| name" + "&nbsp;".repeat(36) + " | description |")
        newReadme.add("| --- | --- |")
        newReadme.addAll(orxMultiplatform)

        newReadme.add("\n## JVM only\n")
        newReadme.add("| name" + "&nbsp;".repeat(36) + " | description |")
        newReadme.add("| --- | --- |")
        newReadme.addAll(orxJVMOnly)

        newReadme.addAll(footer)

        // Write result
        mainReadme.writeText(newReadme.joinToString("\n"))
    }
}

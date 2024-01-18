import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import org.gradle.work.InputChanges
import java.io.File
import java.net.URLClassLoader
import javax.inject.Inject

abstract class CollectScreenshotsTask @Inject constructor() : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:SkipWhenEmpty
    abstract val inputDir: DirectoryProperty

    @get:InputFiles
    abstract val runtimeDependencies: Property<FileCollection>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val ignore: ListProperty<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val preloadClass = File(project.rootProject.projectDir, "buildSrc/build/classes/kotlin/preload")
        require(preloadClass.exists()) {
            "preload class not found: '${preloadClass.absolutePath}'"
        }
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach
            if (change.file.extension == "class") {
                val klassName = change.file.nameWithoutExtension
                if (klassName.dropLast(2) in ignore.get()) {
                    return@forEach
                }

                try {
                    val classParentDir = change.file.parentFile
                    val cp = (runtimeDependencies.get().map { it.toURI().toURL() } + classParentDir.toURI()
                        .toURL()).toTypedArray()
                    val ucl = URLClassLoader(cp)
                    val klass = ucl.loadClass(klassName)
                    klass.getMethod("main")
                } catch (e: NoSuchMethodException) {
                    return@forEach
                }

                println("Collecting screenshot for ${klassName}")

                execOperations.javaexec {
                    this.classpath += project.files(inputDir.get().asFile, preloadClass)
                    this.classpath += runtimeDependencies.get()
                    this.mainClass.set(klassName)
                    this.workingDir(project.rootProject.projectDir)
                    this.jvmArgs(
                        "-DtakeScreenshot=true",
                        "-DscreenshotPath=${outputDir.get().asFile}/$klassName.png",
                        "-Dorg.openrndr.exceptions=JVM"
                    )
                }
            }
        }
        // this is only executed if there are changes in the inputDir
        val runDemos = outputDir.get().asFile.listFiles { file: File ->
            file.extension == "png"
        }!!.map { it.nameWithoutExtension }.sorted()
        val readme = File(project.projectDir, "README.md")
        if (readme.exists()) {
            var lines = readme.readLines().toMutableList()
            val screenshotsLine = lines.indexOfFirst { it == "<!-- __demos__ -->" }
            if (screenshotsLine != -1) {
                lines = lines.subList(0, screenshotsLine)
            }
            lines.add("<!-- __demos__ -->")
            lines.add("## Demos")

            // Find out if current project is MPP
            val demoModuleName = if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                "jvmDemo"
            } else {
                "demo"
            }

            for (demo in runDemos) {
                val projectPath = project.projectDir.relativeTo(project.rootDir)
                lines.add("### ${demo.dropLast(2)}")
                lines.add("[source code](src/${demoModuleName}/kotlin/${demo.dropLast(2)}.kt)")
                lines.add("")
                lines.add("![${demo}](https://raw.githubusercontent.com/openrndr/orx/media/$projectPath/images/${demo}.png)")
                lines.add("")
            }
            readme.delete()
            readme.writeText(lines.joinToString("\n"))
        }
    }
}

object ScreenshotsHelper {
    fun collectScreenshots(
        project: Project,
        sourceSet: SourceSet,
        config: CollectScreenshotsTask.() -> Unit
    ): CollectScreenshotsTask {
        val task = project.tasks.register<CollectScreenshotsTask>("collectScreenshots").get()
        task.outputDir.set(project.file(project.projectDir.toString() + "/images"))
        task.inputDir.set(File(project.layout.buildDirectory.get().asFile, "classes/kotlin/${sourceSet.name}"))
        task.runtimeDependencies.set(sourceSet.runtimeClasspath)
        task.config()
        task.dependsOn(sourceSet.output)
        return task
    }
}


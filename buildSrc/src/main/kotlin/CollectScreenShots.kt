import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import java.io.File
import java.net.URLClassLoader
import javax.inject.Inject

abstract class CollectScreenshotsTask @Inject constructor() : DefaultTask() {
    @get:Incremental
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:InputDirectory
    abstract val inputDir: DirectoryProperty

    @get:Input
    abstract val runtimeDependencies: Property<FileCollection>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val ignore: ListProperty<String>


    init {
        ignore.set(emptyList())
    }
    @TaskAction
    fun execute(inputChanges: InputChanges) {
        val preloadClass = File(project.rootProject.projectDir, "buildSrc/build/classes/kotlin/preload")
        require(preloadClass.exists()) {
            "preload class not found: '${preloadClass.absolutePath}'"

        }
        inputChanges.getFileChanges(inputDir).forEach { change ->
            println(change)
            if (change.fileType == FileType.DIRECTORY) return@forEach
            if (change.file.extension == "class") {
                val klassName = change.file.nameWithoutExtension
                if (klassName.dropLast(2) in ignore.get())
                    return@forEach

                val cp = (runtimeDependencies.get().map { it.toURI().toURL() } +
                        inputDir.get().asFile.toURI().toURL()
                        )
                    .toTypedArray()

                val ucl = URLClassLoader(cp)
                val klass = ucl.loadClass(klassName)
                println("Collecting screenshot for ${klassName} ${klass}")

                try {
                    val mainMethod = klass.getMethod("main")
                    project.javaexec {
                        this.classpath += project.files(inputDir.get().asFile, preloadClass)
                        this.classpath += runtimeDependencies.get()
                        this.mainClass.set(klassName)
                        this.workingDir(project.rootProject.projectDir)
                        jvmArgs("-DtakeScreenshot=true", "-DscreenshotPath=${outputDir.get().asFile}/$klassName.png -Dorg.openrndr.exceptions=JVM")
                    }
                } catch (e: NoSuchMethodException) {
                    // silently ignore
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        // this is only executed if there are changes in the inputDir
        val runDemos = outputDir.get().asFile.listFiles { file: File ->
            file.extension == "png"
        }.map { it.nameWithoutExtension }.sorted()
        val readme = File(project.projectDir, "README.md")
        if (readme.exists()) {
            var lines = readme.readLines().toMutableList()
            val screenshotsLine = lines.indexOfFirst { it == "<!-- __demos__ -->" }
            if (screenshotsLine != -1) {
                lines = lines.subList(0, screenshotsLine)
            }
            lines.add("<!-- __demos__ -->")
            lines.add("## Demos")
            for (demo in runDemos) {
                val projectPath = project.projectDir.relativeTo(project.rootDir)
                lines.add("### ${demo.dropLast(2)}")
                lines.add("[source code](src/demo/kotlin/${demo.dropLast(2)}.kt)")
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
    fun KotlinJvmCompilation.collectScreenshots(config: CollectScreenshotsTask.() -> Unit): CollectScreenshotsTask {
        val task = this.project.tasks.register<CollectScreenshotsTask>("collectScreenshots").get()
        task.outputDir.set(project.file(project.projectDir.toString() + "/images"))
        task.inputDir.set(output.classesDirs.first())
        task.runtimeDependencies.set(runtimeDependencyFiles)
        task.config()
        task.dependsOn(this.compileKotlinTask)
        return task

    }

    fun collectScreenshots(project: Project, sourceSet: SourceSet, config: CollectScreenshotsTask.() -> Unit): CollectScreenshotsTask {
        val task = project.tasks.register<CollectScreenshotsTask>("collectScreenshots").get()
        task.outputDir.set(project.file(project.projectDir.toString() + "/images"))
        task.inputDir.set(File(project.buildDir, "classes/kotlin/${sourceSet.name}"))
        task.runtimeDependencies.set(sourceSet.runtimeClasspath)
        task.config()
        task.dependsOn(sourceSet.output)
        return task
    }
}


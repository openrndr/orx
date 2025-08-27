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

private class CustomClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    fun findClass(file: File): Class<*> = defineClass(null, file.readBytes(), 0, file.readBytes().size)
}

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

        // Execute demos
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach
            if (change.file.extension == "class") {
                var klassName = change.file.nameWithoutExtension
                if (klassName.dropLast(2) in ignore.get()) {
                    return@forEach
                }
                try {
                    val cp = (runtimeDependencies.get().map { it.toURI().toURL() } + inputDir.get().asFile.toURI()
                        .toURL()).toTypedArray()
                    val ucl = URLClassLoader(cp)
                    val ccl = CustomClassLoader(ucl)
                    val tempClass = ccl.findClass(change.file)
                    klassName = tempClass.name
                    val klass = ucl.loadClass(klassName)
                    klass.getMethod("main")
                } catch (e: NoSuchMethodException) {
                    return@forEach
                }

                println("Collecting screenshot for $klassName")
                val imageName = klassName.replace(".", "-")
                val pngFile = "${outputDir.get().asFile}/$imageName.png"

                fun launchDemoProgram() {
                    execOperations.javaexec {
                        this.classpath += project.files(inputDir.get().asFile, preloadClass)
                        this.classpath += runtimeDependencies.get()
                        this.mainClass.set(klassName)
                        this.workingDir(project.rootProject.projectDir)
                        this.jvmArgs(
                            "-DtakeScreenshot=true",
                            "-DscreenshotPath=$pngFile",
                            "-Dorg.openrndr.exceptions=JVM",
                            "-Dorg.openrndr.gl3.debug=true",
                            "-Dorg.openrndr.gl3.delete_angle_on_exit=false"
                        )
                    }
                }

                // A. Create an empty image for quick tests
                File(pngFile).createNewFile()

                // B. Create an actual image by running a demo program
//                runCatching {
//                    launchDemoProgram()
//                }.onFailure {
//                    println("Retrying $klassName after error: ${it.message}")
//                    Thread.sleep(5000)
//                    launchDemoProgram()
//                }
            }
        }

        // List found PNG images.
        // Only executed if there are changes in the inputDir.
        val demoImageBaseNames = outputDir.get().asFile.listFiles { file: File ->
            file.extension == "png"
        }!!.sortedBy { it.absolutePath.lowercase() }.map { it.nameWithoutExtension }

        // Update readme.md using the found PNG images
        val readme = File(project.projectDir, "README.md")
        if (readme.exists()) {
            var readmeLines = readme.readLines().toMutableList()
            val screenshotsLine = readmeLines.indexOfFirst { it == "<!-- __demos__ -->" }
            if (screenshotsLine != -1) {
                readmeLines = readmeLines.subList(0, screenshotsLine)
            }
            readmeLines.add("<!-- __demos__ -->")
            readmeLines.add("## Demos")

            val isKotlinMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
            val demoModuleName = if (isKotlinMultiplatform) "jvmDemo" else "demo"

            for (demoImageBaseName in demoImageBaseNames) {
                val projectPath = project.projectDir.relativeTo(project.rootDir)

                // val url = "" // for local testing
                val url = "https://raw.githubusercontent.com/openrndr/orx/media/$projectPath/"

                val imagePath = demoImageBaseName.dropLast(2).replace("-", "/")
                val ktFilePath = "src/$demoModuleName/kotlin/$imagePath.kt"
                val ktFile = File("$projectPath/$ktFilePath")

                val description = if (ktFile.isFile) {
                    val codeLines = ktFile.readLines()
                    val start = codeLines.indexOfFirst { it.startsWith("/**") }
                    val end = codeLines.indexOfFirst { it.endsWith("*/") }
                    val main = codeLines.indexOfFirst { it.startsWith("fun main") }

                    if ((start < end) && (end < main)) {
                        codeLines.subList(start + 1, end).joinToString("\n") {
                            it.trimStart(' ', '*')
                        }
                    } else ""
                } else ""

                readmeLines.add(
                    """
                    |### $imagePath
                    |
                    |$description
                    |
                    |![$demoImageBaseName](${url}images/$demoImageBaseName.png)
                    |
                    |[source code]($ktFilePath)
                    |
                    """.trimMargin()
                )
            }
            readme.delete()
            readme.writeText(readmeLines.joinToString("\n"))
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

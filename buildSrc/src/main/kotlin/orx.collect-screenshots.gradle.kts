import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import java.net.URLClassLoader

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
        inputChanges.getFileChanges(inputDir).forEach { change ->
            if (change.fileType == FileType.DIRECTORY) return@forEach
            if (change.file.extension == "class" && !(change.file.name.contains("$"))) {
                val klassName = change.file.nameWithoutExtension
                if (klassName.dropLast(2) in ignore.get())
                    return@forEach

                val cp = (runtimeDependencies.get().map { it.toURI().toURL() } + inputDir.get().asFile.toURI().toURL())
                    .toTypedArray()

                val ucl = URLClassLoader(cp)




                val klass = ucl.loadClass(klassName)
                println("Collecting screenshot for ${klassName} ${klass}")

                val mainMethod = klass.getMethod("main")
                println(mainMethod)
                project.javaexec {
                    this.classpath += project.files(inputDir.get().asFile)
                    this.classpath += runtimeDependencies.get()
                    this.mainClass.set(klassName)
                    this.workingDir(project.rootProject.projectDir)
                    jvmArgs("-DtakeScreenshot=true", "-DscreenshotPath=${outputDir.get().asFile}/$klassName.png")
                }
            }
        }
        // this is only executed if there are chances in the inputDir
        val runDemos = outputDir.get().asFile.listFiles { file: File ->
            file.extension == "png"
        }.map { it.nameWithoutExtension }
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
                lines.add("### ${demo.dropLast(2)}")
                lines.add("[source code](src/demo/kotlin/${demo.dropLast(2)}.kt)")
                lines.add("")
                lines.add("![${demo}](https://raw.githubusercontent.com/openrndr/orx/media/${project.name}/images/${demo}.png)")
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
        return task
    }
}


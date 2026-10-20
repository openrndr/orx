import org.gradle.api.flow.FlowAction
import org.gradle.api.flow.FlowParameters
import org.gradle.api.flow.FlowScope
import org.gradle.api.plugins.HelpTasksPlugin
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import javax.inject.Inject

plugins {
    alias(libs.plugins.nebula.release)
    alias(libs.plugins.nmcp)
    id("org.openrndr.extra.convention.dokka")
}

repositories {
    mavenCentral()
}

val mainReadmePath: String = file("README.md").path
val subProjectData: List<Map<String, String>> = subprojects.mapNotNull { sub ->
    val readmeFile = sub.file("README.md")
    if (readmeFile.exists()) {
        mapOf(
            "name" to sub.name,
            "path" to sub.path.substring(1).replace(":", "/"),
            "readmePath" to readmeFile.path
        )
    } else null
}

tasks.register("buildMainReadme") {
    // local copies, so the task action doesn't capture the script object
    val subProjectData = subProjectData
    val mainReadmePath = mainReadmePath
    inputs.property("subProjectData", subProjectData)
    inputs.property("mainReadmePath", mainReadmePath)

    doFirst {
        // Load README.md and find [begin, end] section to replace
        val mainReadme = File(mainReadmePath)
        val lines = mainReadme.readLines()

        val begin = lines.indexOfFirst { it == "<!-- __orxListBegin__ -->" }
        val end = lines.indexOfFirst { it == "<!-- __orxListEnd__ -->" }
        if (begin == -1 || end == -1) {
            println("Comments for orx list generation not found in README.md!")
            return@doFirst
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

        // Build orx list
        for (sub in subProjectData) {
            val orxReadmeFile = File(sub.getValue("readmePath"))
            if (orxReadmeFile.exists()) {
                val match = descriptionRx.find(orxReadmeFile.readText()) ?: continue
                val trimmedDescription = match.groupValues[1].trim().replace("\n", " ").replace("\r", "")
                val row = "| [`${sub.getValue("name")}`](${sub.getValue("path")}/) | $trimmedDescription |"
                if (sub.getValue("path").startsWith("orx-jvm")) orxJVMOnly.add(row) else orxMultiplatform.add(row)
            } else {
                println("${sub.getValue("name")}/README.md not found!")
            }
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
group = "org.openrndr.extra"

nmcpAggregation {
    centralPortal {
        username.set(findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME"))
        password.set(findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD"))

        // publish manually from the portal
        publishingType = "USER_MANAGED"
    }
}

/**
 * The projects published to Maven Central. This is an explicit list because
 * publishAllProjectsProbablyBreakingProjectIsolation() passes Project objects as dependencies, which Gradle 10 turns
 * into an error. The check in projectsEvaluated below keeps it complete.
 */
val publishedProjects = listOf(
    ":orx-bvh",
    ":orx-camera",
    ":orx-color",
    ":orx-composition",
    ":orx-compositor",
    ":orx-delegate-magic",
    ":orx-depth-camera",
    ":orx-easing",
    ":orx-envelopes",
    ":orx-expression-evaluator",
    ":orx-expression-evaluator-typed",
    ":orx-fcurve",
    ":orx-fft",
    ":orx-force-2d",
    ":orx-fx",
    ":orx-g-code",
    ":orx-gradient-descent",
    ":orx-hash-grid",
    ":orx-image-fit",
    ":orx-integral-image",
    ":orx-interval-tree",
    ":orx-jumpflood",
    ":orx-jvm:orx-axidraw",
    ":orx-jvm:orx-boofcv",
    ":orx-jvm:orx-chataigne",
    ":orx-jvm:orx-crash-handler",
    ":orx-jvm:orx-depth-camera-calibrator",
    ":orx-jvm:orx-dnk3",
    ":orx-jvm:orx-file-watcher",
    ":orx-jvm:orx-git-archiver",
    ":orx-jvm:orx-git-archiver-gradle",
    ":orx-jvm:orx-gui",
    ":orx-jvm:orx-keyframer",
    ":orx-jvm:orx-kinect-common",
    ":orx-jvm:orx-kinect-v1",
    ":orx-jvm:orx-kinect-v1-demo",
    ":orx-jvm:orx-kotlin-parser",
    ":orx-jvm:orx-midi",
    ":orx-jvm:orx-minim",
    ":orx-jvm:orx-olive",
    ":orx-jvm:orx-osc",
    ":orx-jvm:orx-panel",
    ":orx-jvm:orx-poisson-fill",
    ":orx-jvm:orx-processing",
    ":orx-jvm:orx-python",
    ":orx-jvm:orx-rabbit-control",
    ":orx-jvm:orx-realsense2",
    ":orx-jvm:orx-remote-control",
    ":orx-jvm:orx-syphon",
    ":orx-jvm:orx-video-profiles",
    ":orx-kdtree",
    ":orx-marching-squares",
    ":orx-math",
    ":orx-mesh",
    ":orx-mesh-generators",
    ":orx-mesh-noise",
    ":orx-module-catalog",
    ":orx-no-clear",
    ":orx-noise",
    ":orx-obj-loader",
    ":orx-palette",
    ":orx-parameters",
    ":orx-property-watchers",
    ":orx-quadtree",
    ":orx-shade-styles",
    ":orx-shader-phrases",
    ":orx-shapes",
    ":orx-svg",
    ":orx-temporal-blur",
    ":orx-text-on-contour",
    ":orx-text-writer",
    ":orx-time-operators",
    ":orx-timer",
    ":orx-triangulation",
    ":orx-turtle",
    ":orx-view-box",
)

dependencies {
    for (path in publishedProjects) {
        nmcpAggregation(project(path))
    }
    subprojects.filter {
        it.name.startsWith("orx-") && !it.name.contains("-catalog")
    }.forEach { subproject ->
        dokka(project(subproject.path))
    }
}

// nmcp resolves the aggregation leniently, so a project missing from publishedProjects would silently be left out of
// the upload. Not compatible with project isolation, which nebula-release doesn't support either.
gradle.projectsEvaluated {
    val problems = mutableListOf<String>()
    // a listed path that doesn't exist already fails in project(path) above
    for (path in publishedProjects) {
        if (!project(path).plugins.hasPlugin("com.gradleup.nmcp")) {
            problems += "$path is listed in publishedProjects but doesn't apply com.gradleup.nmcp"
        }
    }
    for (sub in subprojects) {
        val publications = sub.extensions.findByType<PublishingExtension>()?.publications
        if (!publications.isNullOrEmpty() && sub.path !in publishedProjects) {
            problems += "${sub.path} has publications but is missing from publishedProjects"
        }
    }
    if (problems.isNotEmpty()) {
        throw GradleException("nmcp aggregation is out of sync:\n" + problems.joinToString("\n") { "  - $it" })
    }
}

subprojects {
    tasks.register<DependencyReportTask>("dependenciesAll") {
        group = HelpTasksPlugin.HELP_GROUP
        description = "Displays all dependencies, including subprojects."
    }
}

abstract class VersionPrinterFlowAction : FlowAction<VersionPrinterFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:Input
        val version: Property<String>
    }

    override fun execute(parameters: Parameters) {
        println("\n")
        println("orx = \"${parameters.version.get()}\"")
    }
}

// FlowScope.always() is the configuration-cache-compatible replacement for
// the deprecated `gradle.buildFinished` hook: it fires once, at the very end
// of every build (cache hit or not), regardless of build outcome.
abstract class VersionPrinterPlugin : Plugin<Project> {
    @get:Inject
    abstract val flowScope: FlowScope

    override fun apply(project: Project) {
        flowScope.always(VersionPrinterFlowAction::class.java) {
            parameters.version.set(project.provider { project.version.toString() })
        }
    }
}

apply<VersionPrinterPlugin>()

plugins {
    `version-catalog`
    `maven-publish`
    signing
}

catalog {
    versionCatalog {
        library("camera", "org.openrndr.extra:orx-camera:$version")
        library("color", "org.openrndr.extra:orx-color:$version")
        library("composition", "org.openrndr.extra:orx-composition:$version")
        library("compositor", "org.openrndr.extra:orx-compositor:$version")
        library("delegate-magic", "org.openrndr.extra:orx-delegate-magic:$version")
        library("depth-camera", "org.openrndr.extra:orx-depth-camera:$version")
        library("easing", "org.openrndr.extra:orx-easing:$version")
        library("envelopes", "org.openrndr.extra:orx-envelopes:$version")
        library("expression-evaluator", "org.openrndr.extra:orx-expression-evaluator:$version")
        library("expression-evaluator-typed", "org.openrndr.extra:orx-expression-evaluator-typed:$version")
        library("fcurve", "org.openrndr.extra:orx-fcurve:$version")
        library("fft", "org.openrndr.extra:orx-fft:$version")
        library("fx", "org.openrndr.extra:orx-fx:$version")
        library("gradient-descent", "org.openrndr.extra:orx-gradient-descent:$version")
        library("hash-grid", "org.openrndr.extra:orx-hash-grid:$version")
        library("image-fit", "org.openrndr.extra:orx-image-fit:$version")
        library("integral-image", "org.openrndr.extra:orx-integral-image:$version")
        library("interval-tree", "org.openrndr.extra:orx-interval-tree:$version")
        library("jumpflood", "org.openrndr.extra:orx-jumpflood:$version")
        library("axidraw", "org.openrndr.extra:orx-axidraw:$version")
        library("boofcv", "org.openrndr.extra:orx-boofcv:$version")
        library("chataigne", "org.openrndr.extra:orx-chataigne:$version")
        library("depth-camera-calibrator", "org.openrndr.extra:orx-depth-camera-calibrator:$version")
        library("dnk3", "org.openrndr.extra:orx-dnk3:$version")
        library("file-watcher", "org.openrndr.extra:orx-file-watcher:$version")
        library("git-archiver", "org.openrndr.extra:orx-git-archiver:$version")
        //library("git-archiver-gradle", "org.openrndr.extra:orx-git-archiver-gradle:$version")
        library("gui", "org.openrndr.extra:orx-gui:$version")
        library("keyframer", "org.openrndr.extra:orx-keyframer:$version")
        library("kinect-v1-core", "org.openrndr.extra:orx-kinect-v1:$version")
        library("kotlin-parser", "org.openrndr.extra:orx-kotlin-parser:$version")
        library("midi", "org.openrndr.extra:orx-midi:$version")
        library("minim", "org.openrndr.extra:orx-minim:$version")
        library("olive", "org.openrndr.extra:orx-olive:$version")
        library("osc", "org.openrndr.extra:orx-osc:$version")
        library("panel", "org.openrndr.extra:orx-panel:$version")
        library("poisson-fill", "org.openrndr.extra:orx-poisson-fill:$version")
        library("processing", "org.openrndr.extra:orx-processing:$version")
        library("rabbit-control", "org.openrndr.extra:orx-rabbit-control:$version")
        library("realsense2", "org.openrndr.extra:orx-realsense2:$version")
        library("syphon", "org.openrndr.extra:orx-syphon:$version")
        library("video-profiles", "org.openrndr.extra:orx-video-profiles:$version")
        library("kdtree", "org.openrndr.extra:orx-kdtree:$version")
        library("marching-squares", "org.openrndr.extra:orx-marching-squares:$version")
        library("math", "org.openrndr.extra:orx-math:$version")
        library("mesh-core", "org.openrndr.extra:orx-mesh:$version")
        library("mesh-generators", "org.openrndr.extra:orx-mesh-generators:$version")
        library("mesh-noise", "org.openrndr.extra:orx-mesh-noise:$version")
        library("no-clear", "org.openrndr.extra:orx-no-clear:$version")
        library("noise", "org.openrndr.extra:orx-noise:$version")
        library("obj-loader", "org.openrndr.extra:orx-obj-loader:$version")
        library("palette", "org.openrndr.extra:orx-palette:$version")
        library("parameters", "org.openrndr.extra:orx-parameters:$version")
        library("property-watchers", "org.openrndr.extra:orx-property-watchers:$version")
        library("quadtree", "org.openrndr.extra:orx-quadtree:$version")
        library("shade-styles", "org.openrndr.extra:orx-shade-styles:$version")
        library("shader-phrases", "org.openrndr.extra:orx-shader-phrases:$version")
        library("shapes", "org.openrndr.extra:orx-shapes:$version")
        library("svg", "org.openrndr.extra:orx-svg:$version")
        library("temporal-blur", "org.openrndr.extra:orx-temporal-blur:$version")
        library("text-on-contour", "org.openrndr.extra:orx-text-on-contour:$version")
        library("text-writer", "org.openrndr.extra:orx-text-writer:$version")
        library("time-operators", "org.openrndr.extra:orx-time-operators:$version")
        library("timer", "org.openrndr.extra:orx-timer:$version")
        library("triangulation", "org.openrndr.extra:orx-triangulation:$version")
        library("turtle", "org.openrndr.extra:orx-turtle:$version")
        library("view-box", "org.openrndr.extra:orx-view-box:$version")
        library("gcode", "org.openrndr.extra:orx-g-code:$version")

        bundle(
            "basic",
            listOf(
                "camera",
                "color",
                "composition",
                "compositor",
                "fx",
                "image-fit",
                "panel",
                "video-profiles",
                "math",
                "mesh-generators",
                "no-clear",
                "noise",
                "shade-styles",
                "shader-phrases",
                "shapes",
                "svg",
                "text-on-contour",
                "text-writer"
            )
        )
    }
}

group = "org.openrndr.extra"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.openrndr.extra"
            artifactId = "orx-module-catalog"
            description = "ORX module catalog"
            from(components["versionCatalog"])
            pom {
                name.set(project.name)
                description.set(project.name)
                url.set("https://openrndr.org")
                developers {
                    developer {
                        id.set("edwinjakobs")
                        name.set("Edwin Jakobs")
                        email.set("edwin@openrndr.org")
                    }
                }

                licenses {
                    license {
                        name.set("BSD-2-Clause")
                        url.set("https://github.com/openrndr/orx/blob/master/LICENSE")
                        distribution.set("repo")
                    }
                }

                scm {
                    connection.set("scm:git:git@github.com:openrndr/orx.git")
                    developerConnection.set("scm:git:ssh://github.com/openrndr/orx.git")
                    url.set("https://github.com/openrndr/orx")
                }
            }
        }
    }
}

signing {
    val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")
    setRequired({ isReleaseVersion && gradle.taskGraph.hasTask("publish") })
    sign(publishing.publications)
}
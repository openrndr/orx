plugins {
    id("org.openrndr.extra.convention.kotlin-jvm")
    `java-gradle-plugin`
}

apply(plugin = "maven-publish")

dependencies {
    implementation(project(":orx-jvm:orx-git-archiver"))
}

gradlePlugin {
    plugins {
        create("gitArchiveToMarkdown") {
            id = "org.openrndr.extra.gitarchiver.tomarkdown"
            implementationClass = "org.openrndr.extra.gitarchiver.GitArchiveToMarkdown"
        }
    }
}

publishing {
    afterEvaluate {
        publications {
            withType(MavenPublication::class) {
                pom {
                    name.set("gitarchiver tomarkdown")
                    description.set("gitarchiver to markdown gradle plugin")
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
                            url.set("https://github.com/openrndr/openrndr/blob/master/LICENSE")
                            distribution.set("repo")
                        }
                    }

                    scm {
                        connection.set("scm:git:git@github.com:openrndr/orx.git")
                        developerConnection.set("scm:git:ssh://github.com/openrndr/orx.git")
                        url.set("https://github.com/openrndr/openrndr")
                    }
                }
            }
        }
    }

}
val isReleaseVersion = !(version.toString()).endsWith("SNAPSHOT")
signing {
    setRequired({ isReleaseVersion && gradle.taskGraph.hasTask("publish") })
    sign(publishing.publications)
}
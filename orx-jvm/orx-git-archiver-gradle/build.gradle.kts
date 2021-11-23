plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

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
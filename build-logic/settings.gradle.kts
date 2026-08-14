include("orx-convention", "orx-variant-plugin")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal {
            content {
                includeGroup("org.openrndr")
            }
        }
    }

    versionCatalogs {
        val versionsTomlFile = settingsDir.parentFile.resolve("gradle/libs.versions.toml")
        create("libs") {
            from(files(versionsTomlFile))
        }

        // We use a regex to get the openrndr version from the primary catalog as there is no public Gradle API to parse catalogs.
        val regEx = Regex("^openrndr[ ]*=[ ]*(?:\\{[ ]*require[ ]*=[ ]*)?\"(.*)\"[ ]*(?:\\})?", RegexOption.MULTILINE)
        val openrndrVersion = regEx.find(versionsTomlFile.readText())?.groupValues?.get(1) ?: error("can't find openrndr version")
        create("sharedLibs") {
            from("org.openrndr:openrndr-dependency-catalog:$openrndrVersion")
        }
        create("openrndr") {
            from("org.openrndr:openrndr-module-catalog:$openrndrVersion")
        }
    }
}


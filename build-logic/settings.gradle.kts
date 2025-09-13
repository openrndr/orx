include("orx-convention")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal {
            include("org.openrndr")
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }

        // We use a regex to get the openrndr version from the primary catalog as there is no public Gradle API to parse catalogs.
        val regEx = Regex("^openrndr[ ]*=[ ]*(?:\\{[ ]*require[ ]*=[ ]*)?\"(.*)\"[ ]*(?:\\})?", RegexOption.MULTILINE)
        val openrndrVersion = regEx.find(File(rootDir,"../gradle/libs.versions.toml").readText())?.groupValues?.get(1) ?: error("can't find openrndr version")
        create("sharedLibs") {
            from("org.openrndr:openrndr-dependency-catalog:$openrndrVersion")
        }
        create("openrndr") {
            from("org.openrndr:openrndr-module-catalog:$openrndrVersion")
        }
    }
}


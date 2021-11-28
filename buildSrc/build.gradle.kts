
plugins {
    `kotlin-dsl`
}

sourceSets {
    val preload by creating {
        this.java {
            srcDir("src/preload/kotlin")
        }
    }
    val main by getting {
    }

}

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}


val openrndrVersion = ((findProperty("OPENRNDR.version")?.toString())?:System.getenv("OPENRNDR_VERSION"))?.replace("v", "")  ?: "0.5.1-SNAPSHOT"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
    val preloadImplementation by configurations.getting {  }
    preloadImplementation("org.openrndr:openrndr-application:$openrndrVersion")
    preloadImplementation("org.openrndr:openrndr-extensions:$openrndrVersion")
}

tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")

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


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
    val preloadImplementation by configurations.getting {  }
    preloadImplementation("org.openrndr:openrndr-application:0.5.1-SNAPSHOT")
    preloadImplementation("org.openrndr:openrndr-extensions:0.5.1-SNAPSHOT")
}

tasks.all {
    println(this.name)

}
tasks.getByName("compileKotlin").dependsOn("compilePreloadKotlin")
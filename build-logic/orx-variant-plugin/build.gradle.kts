plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}


gradlePlugin {
    plugins {
        create("orxVariants") {
            id = "orx-variant"
            implementationClass = "org.openrndr.extra.variant.plugin.VariantPlugin"
        }
    }
}
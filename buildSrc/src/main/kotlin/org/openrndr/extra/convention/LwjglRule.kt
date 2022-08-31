package org.openrndr.extra.convention

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.named
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import javax.inject.Inject

@CacheableRule
abstract class LwjglRule : ComponentMetadataRule {
    val jvmNativeVariants: List<JvmNativeVariant> = listOf(
        JvmNativeVariant("natives-linux-arm64", OperatingSystemFamily.LINUX, "aarch64"),
        JvmNativeVariant("natives-linux", OperatingSystemFamily.LINUX, "x86-64"),
        JvmNativeVariant("natives-macos-arm64", OperatingSystemFamily.MACOS, "aarch64"),
        JvmNativeVariant("natives-macos", OperatingSystemFamily.MACOS, "x86-64"),
        JvmNativeVariant("natives-windows", OperatingSystemFamily.WINDOWS, "x86-64")
    )

    @get:Inject
    abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) = context.details.run {
        if (id.group != "org.lwjgl") return
        if (id.name == "lwjgl-egl") return
        withVariant("runtime") {
            attributes {
                attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named("none"))
                attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named("none"))
            }
        }
        for ((targetName, os, arch) in jvmNativeVariants) {
            addVariant("$targetName-runtime", "runtime") {
                attributes {
                    attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(os))
                    attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(arch))
                }
                withFiles {
                    addFile("${id.name}-${id.version}-$targetName.jar")
                }
            }
        }
    }
}
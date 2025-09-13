package org.openrndr.extra.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.named
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

data class JvmNativeVariant(val targetName: String, val os: String, val arch: String)

val currentOperatingSystemName: String = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
val currentArchitectureName: String = DefaultNativePlatform.getCurrentArchitecture().name

fun Project.addHostMachineAttributesToRuntimeConfigurations() {
    configurations.matching {
        it.name.endsWith("runtimeClasspath", ignoreCase = true)
    }.configureEach {
        attributes {
            attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(currentOperatingSystemName))
            attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(currentArchitectureName))
        }
    }
}
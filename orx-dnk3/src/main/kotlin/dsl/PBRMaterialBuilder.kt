package org.openrndr.extra.dnk3.dsl

import org.openrndr.extra.dnk3.PBRMaterial

fun pbrMaterial(builder: PBRMaterial.() -> Unit): PBRMaterial {
    return PBRMaterial().apply { builder() }
}

fun test() {
    pbrMaterial {
    }
}
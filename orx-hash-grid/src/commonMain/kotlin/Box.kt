package org.openrndr.extra.hashgrid

import org.openrndr.math.Vector3
import kotlin.jvm.JvmRecord

@JvmRecord
data class Box3D(val corner: Vector3, val width: Double, val height: Double, val depth: Double) {
    companion object {
        val EMPTY = Box3D(Vector3.ZERO, 0.0, 0.0, 0.0)
    }
}
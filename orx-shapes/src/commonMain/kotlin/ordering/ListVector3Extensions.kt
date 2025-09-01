package org.openrndr.extra.shapes.ordering

import org.openrndr.math.Vector3
import org.openrndr.math.map
import org.openrndr.shape.Box
import org.openrndr.shape.bounds
import kotlin.math.max

enum class Axis3DPermutation {
    XYZ,
    XZY,
    YXZ,
    YZX,
    ZXY,
    ZYX
}

fun List<Vector3>.mortonOrder(
    scale: Double = 1.0,
    permutation: Axis3DPermutation = Axis3DPermutation.XYZ,
    bits: Int = 10,
): List<Vector3> {
    val bounds = this.bounds
    val md = max(max(bounds.width, bounds.height), bounds.depth) * scale
    val rbounds = Box(bounds.corner.x, bounds.corner.y, bounds.corner.z, md, md, md)
    val inputPoints = map {
        it.map(
            rbounds.position(0.0, 0.0, 0.0),
            rbounds.position(1.0, 1.0, 1.0),
            Vector3(0.0, 0.0, 0.0),
            Vector3(1023.0, 1023.0, 1023.0)
        )
    }
    val mortonCodes = when (bits) {
        5 -> when (permutation) {
            Axis3DPermutation.XYZ -> inputPoints.map { morton3dEncode5Bit(it.x.toUInt(), it.y.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.XZY -> inputPoints.map { morton3dEncode5Bit(it.x.toUInt(), it.z.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.YXZ -> inputPoints.map { morton3dEncode5Bit(it.y.toUInt(), it.x.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.YZX -> inputPoints.map { morton3dEncode5Bit(it.y.toUInt(), it.z.toUInt(), it.x.toUInt()) }
            Axis3DPermutation.ZXY -> inputPoints.map { morton3dEncode5Bit(it.z.toUInt(), it.x.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.ZYX -> inputPoints.map { morton3dEncode5Bit(it.z.toUInt(), it.y.toUInt(), it.x.toUInt()) }
        }
        10 -> when (permutation) {
            Axis3DPermutation.XYZ -> inputPoints.map { morton3dEncode10Bit(it.x.toUInt(), it.y.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.XZY -> inputPoints.map { morton3dEncode10Bit(it.x.toUInt(), it.z.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.YXZ -> inputPoints.map { morton3dEncode10Bit(it.y.toUInt(), it.x.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.YZX -> inputPoints.map { morton3dEncode10Bit(it.y.toUInt(), it.z.toUInt(), it.x.toUInt()) }
            Axis3DPermutation.ZXY -> inputPoints.map { morton3dEncode10Bit(it.z.toUInt(), it.x.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.ZYX -> inputPoints.map { morton3dEncode10Bit(it.z.toUInt(), it.y.toUInt(), it.x.toUInt()) }
        }
        else -> error("Only 5 and 10 bit modes are supported.")
    }
    return (this zip mortonCodes).sortedBy { it.second }.map { it.first }
}

fun List<Vector3>.hilbertOrder(
    scale: Double = 1.0,
    permutation: Axis3DPermutation = Axis3DPermutation.XYZ,
    bits: Int
): List<Vector3> {
    val bounds = this.bounds
    val md = max(max(bounds.width, bounds.height), bounds.depth) * scale
    val rbounds = Box(bounds.corner.x, bounds.corner.y, bounds.corner.z, md, md, md)
    val inputPoints = map {
        it.map(
            rbounds.position(0.0, 0.0, 0.0),
            rbounds.position(1.0, 1.0, 1.0),
            Vector3(0.0, 0.0, 0.0),
            Vector3(1023.0, 1023.0, 1023.0)
        )
    }
    val hilbertCodes = when(bits) {
        5 -> when (permutation) {
            Axis3DPermutation.XYZ -> inputPoints.map { hilbert3dEncode5Bit(it.x.toUInt(), it.y.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.XZY -> inputPoints.map { hilbert3dEncode5Bit(it.x.toUInt(), it.z.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.YXZ -> inputPoints.map { hilbert3dEncode5Bit(it.y.toUInt(), it.x.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.YZX -> inputPoints.map { hilbert3dEncode5Bit(it.y.toUInt(), it.z.toUInt(), it.x.toUInt()) }
            Axis3DPermutation.ZXY -> inputPoints.map { hilbert3dEncode5Bit(it.z.toUInt(), it.x.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.ZYX -> inputPoints.map { hilbert3dEncode5Bit(it.z.toUInt(), it.y.toUInt(), it.x.toUInt()) }
        }
        10 -> when (permutation) {
            Axis3DPermutation.XYZ -> inputPoints.map { hilbert3dEncode10Bit(it.x.toUInt(), it.y.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.XZY -> inputPoints.map { hilbert3dEncode10Bit(it.x.toUInt(), it.z.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.YXZ -> inputPoints.map { hilbert3dEncode10Bit(it.y.toUInt(), it.x.toUInt(), it.z.toUInt()) }
            Axis3DPermutation.YZX -> inputPoints.map { hilbert3dEncode10Bit(it.y.toUInt(), it.z.toUInt(), it.x.toUInt()) }
            Axis3DPermutation.ZXY -> inputPoints.map { hilbert3dEncode10Bit(it.z.toUInt(), it.x.toUInt(), it.y.toUInt()) }
            Axis3DPermutation.ZYX -> inputPoints.map { hilbert3dEncode10Bit(it.z.toUInt(), it.y.toUInt(), it.x.toUInt()) }
        }
        else -> error("Only 5 and 10 bit modes are supported.")
    }
    return (this zip hilbertCodes).sortedBy { it.second }.map { it.first }
}
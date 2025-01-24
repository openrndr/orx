package org.openrndr.extra.noise.rsequence

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

private const val g1 = 1.618033988749895
private const val a11 = 1.0 / g1

private const val g2 = 1.324717957244746
private const val a21 = 1.0 / g2
private const val a22 = 1.0 / (g2 * g2)

private const val g3 = 1.2207440846057596
private const val a31 = 1.0 / g3
private const val a32 = 1.0 / (g3 * g3)
private const val a33 = 1.0 / (g3 * g3 * g3)

private const val g4 = 1.1673039782614187
private const val a41 = 1.0 / g4
private const val a42 = 1.0 / (g4 * g4)
private const val a43 = 1.0 / (g4 * g4 * g4)
private const val a44 = 1.0 / (g4 * g4 * g4 * g4)

/**
 * Computes the R1 low-discrepancy quasirandom sequence value for a given index as described by Martin Roberts.
 *
 * @param n The index for which the R1 sequence value is to be calculated.
 * @return The R1 sequence value as a Double, providing a low-discrepancy quasirandom number.
 */
fun rSeq1D(n: Int): Double = (0.5 + a11 * n).mod(1.0)

/**
 * Computes the R2 low-discrepancy quasirandom sequence value for a given index as described by Martin Roberts.
 *
 * @param n The index for which the R2 sequence value is to be calculated.
 * @return The R2 sequence value as a [Vector2], providing a low-discrepancy quasirandom number.
 */
fun rSeq2D(n: Int): Vector2 = Vector2(
    (0.5 + a21 * n).mod(1.0),
    (0.5 + a22 * n).mod(1.0)
)

/**
 * Computes the R3 low-discrepancy quasirandom sequence value for a given index as described by Martin Roberts.
 *
 * @param n The index for which the R3 sequence value is to be calculated.
 * @return The R3 sequence value as a [Vector3], providing a low-discrepancy quasirandom number.
 */
fun rSeq3D(n: Int): Vector3 = Vector3(
    (0.5 + a31 * n).mod(1.0),
    (0.5 + a32 * n).mod(1.0),
    (0.5 + a33 * n).mod(1.0)
)

/**
 * Computes the R4 low-discrepancy quasirandom sequence value for a given index as described by Martin Roberts.
 *
 * @param n The index for which the R4 sequence value is to be calculated.
 * @return The R4 sequence value as a [Vector4], providing a low-discrepancy quasirandom number.
 */
fun rSeq4D(n: Int): Vector4 = Vector4(
    (0.5 + a41 * n).mod(1.0),
    (0.5 + a42 * n).mod(1.0),
    (0.5 + a43 * n).mod(1.0),
    (0.5 + a44 * n).mod(1.0)
)

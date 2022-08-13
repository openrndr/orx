package org.openrndr.extra.meshgenerators

import org.openrndr.draw.VertexBuffer
import org.openrndr.math.Spherical
import org.openrndr.math.Vector2

fun sphereMesh(sides: Int = 16, segments: Int = 16, radius: Double = 1.0, invert: Boolean = false): VertexBuffer {
    val vertexCount = 2 * sides * 3 + Math.max(0, (segments - 2)) * sides * 6
    val vb = meshVertexBuffer(vertexCount)
    vb.put {
        generateSphere(sides, segments, radius, invert, bufferWriter(this))
    }
    return vb
}


fun generateSphere(sides: Int, segments: Int, radius: Double = 1.0, invert: Boolean = false, writer: VertexWriter) {
    val inverter = if (invert) -1.0 else 1.0
    for (t in 0 until segments) {
        for (s in 0 until sides) {
            val st00 = Spherical(s * 180.0 * 2.0 / sides, t * 180.0 / segments, radius)
            val st01 = Spherical(s * 180.0 * 2.0 / sides, (t + 1) * 180.0 / segments, radius)
            val st10 = Spherical((s + 1) * 180.0 * 2.0 / sides, t * 180.0 / segments, radius)
            val st11 = Spherical((s + 1) * 180.0 * 2.0 / sides, (t + 1) * 180.0 / segments, radius)

            val thetaMax = 180.0 * 2.0
            val phiMax = 180.0

            when (t) {
                0 -> {
                    writer(st00.cartesian, st00.cartesian.normalized * inverter, Vector2(st00.theta / thetaMax + 0.5, 1.0 - st00.phi / phiMax))
                    writer(st01.cartesian, st01.cartesian.normalized * inverter, Vector2(st01.theta / thetaMax + 0.5, 1.0 - st01.phi / phiMax))
                    writer(st11.cartesian, st11.cartesian.normalized * inverter, Vector2(st11.theta / thetaMax + 0.5, 1.0 - st11.phi / phiMax))
                }
                segments - 1 -> {
                    writer(st11.cartesian, st11.cartesian.normalized * inverter, Vector2(st11.theta / thetaMax + 0.5, 1.0 - st11.phi / phiMax))
                    writer(st10.cartesian, st10.cartesian.normalized * inverter, Vector2(st10.theta / thetaMax + 0.5, 1.0 - st10.phi / phiMax))
                    writer(st00.cartesian, st00.cartesian.normalized * inverter, Vector2(st00.theta / thetaMax + 0.5, 1.0 - st00.phi / phiMax))
                }
                else -> {
                    writer(st00.cartesian, st00.cartesian.normalized * inverter, Vector2(st00.theta / thetaMax + 0.5, 1.0 - st00.phi / phiMax))
                    writer(st01.cartesian, st01.cartesian.normalized * inverter, Vector2(st01.theta / thetaMax + 0.5, 1.0 - st01.phi / phiMax))
                    writer(st11.cartesian, st11.cartesian.normalized * inverter, Vector2(st11.theta / thetaMax + 0.5, 1.0 - st11.phi / phiMax))

                    writer(st11.cartesian, st11.cartesian.normalized * inverter, Vector2(st11.theta / thetaMax + 0.5, 1.0 - st11.phi / phiMax))
                    writer(st10.cartesian, st10.cartesian.normalized * inverter, Vector2(st10.theta / thetaMax + 0.5, 1.0 - st10.phi / phiMax))
                    writer(st00.cartesian, st00.cartesian.normalized * inverter, Vector2(st00.theta / thetaMax + 0.5, 1.0 - st00.phi / phiMax))
                }
            }
        }
    }
}

fun generateHemisphere(sides: Int, segments: Int, radius: Double = 1.0, invert: Boolean = false, writer: VertexWriter) {
    val inverter = if (invert) -1.0 else 1.0
    for (t in 0 until segments) {
        for (s in 0 until sides) {
            val st00 = Spherical(s * 180.0 * 2.0 / sides, t * 180.0 / segments, radius)
            val st01 = Spherical(s * 180.0 * 2.0 / sides, (t + 1) * 180.0 / segments, radius)
            val st10 = Spherical((s + 1) * 180.0 * 2.0 / sides, t * 180.0 / segments, radius)
            val st11 = Spherical((s + 1) * 180.0 * 2.0 / sides, (t + 1) * 180.0 / segments, radius)

            val thetaMax = 180.0 * 2.0
            val phiMax = 180.0 * 0.5

            when (t) {
                0 -> {
                    writer(st00.cartesian, st00.cartesian.normalized * inverter, Vector2(st00.theta / thetaMax + 0.5, 1.0 - st00.phi / phiMax))
                    writer(st01.cartesian, st01.cartesian.normalized * inverter, Vector2(st01.theta / thetaMax + 0.5, 1.0 - st01.phi / phiMax))
                    writer(st11.cartesian, st11.cartesian.normalized * inverter, Vector2(st11.theta / thetaMax + 0.5, 1.0 - st11.phi / phiMax))
                }
                else -> {
                    writer(st00.cartesian, st00.cartesian.normalized * inverter, Vector2(st00.theta / thetaMax + 0.5, 1.0 - st00.phi / phiMax))
                    writer(st01.cartesian, st01.cartesian.normalized * inverter, Vector2(st01.theta / thetaMax + 0.5, 1.0 - st01.phi / phiMax))
                    writer(st11.cartesian, st11.cartesian.normalized * inverter, Vector2(st11.theta / thetaMax + 0.5, 1.0 - st11.phi / phiMax))

                    writer(st11.cartesian, st11.cartesian.normalized * inverter, Vector2(st11.theta / thetaMax + 0.5, 1.0 - st11.phi / phiMax))
                    writer(st10.cartesian, st10.cartesian.normalized * inverter, Vector2(st10.theta / thetaMax + 0.5, 1.0 - st10.phi / phiMax))
                    writer(st00.cartesian, st00.cartesian.normalized * inverter, Vector2(st00.theta / thetaMax + 0.5, 1.0 - st00.phi / phiMax))
                }
            }
        }
    }
}
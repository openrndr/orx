package org.openrndr.extra.noise

import org.openrndr.math.Polar
import org.openrndr.math.Vector2

/**
 * Polar coordinate front-end for 2D noise functions
 */
fun polarFunc(
    noise: (Int, Double, Double) -> Double,
    origin: Vector2 = Vector2.ZERO,
): (seed: Int, polar: Polar) -> Double {
    return { seed, polar ->
        val c = polar.cartesian + origin
        noise(seed, c.x, c.y)
    }
}

/**
 * Polar coordinate front-end for 2D noise functions
 */
fun fixedRadiusPolarFunc(
    noise: (Int, Double, Double) -> Double,
    radius: Double,
    origin: Vector2 = Vector2.ZERO,
): (seed: Int, angleInDegrees: Double) -> Double {
    return { seed, angleInDegrees ->
        val c = Polar(angleInDegrees, radius).cartesian + origin
        noise(seed, c.x, c.y)
    }
}

/**
 * Polar coordinate front-end for 2D noise functions with variable offset
 */
fun polarOffsetFunc(
    noise: (Int, Double, Double) -> Double,
    origin: Vector2 = Vector2.ZERO,

): (seed: Int, polar: Polar, offset: Vector2) -> Double {
    return { seed, polar, offset ->
        val c = polar.cartesian + origin + offset
        noise(seed, c.x, c.y)
    }
}

fun ((Int, Double, Double) -> Double).withPolarInput(origin: Vector2 = Vector2.ZERO): (Int, Polar) -> Double =
    polarFunc(this, origin)

fun ((Int, Vector2) -> Double).withPolarInput(origin: Vector2 = Vector2.ZERO): (Int, Polar) -> Double =
    polarFunc(this.withScalarInput(), origin)


fun ((Int, Double, Double) -> Double).withPolarOffsetInput(origin: Vector2 = Vector2.ZERO): (Int, Polar, Vector2) -> Double =
    polarOffsetFunc(this, origin)


fun ((Int, Vector2) -> Double).withPolarOffsetInput(origin: Vector2 = Vector2.ZERO): (Int, Polar, Vector2) -> Double =
    polarOffsetFunc(this.withScalarInput(), origin)



fun ((Int, Double, Double) -> Double).fixedRadiusPolar(
    radius: Double,
    origin: Vector2 = Vector2.ZERO
): (Int, Double) -> Double =
    fixedRadiusPolarFunc(this, radius, origin)

private fun example() {
    val polarFbmSimplex = polarFunc(noise = fbmFunc2D(noise = ::simplex))
    val polarBillowPerlin = polarFunc(noise = billowFunc2D(noise = ::perlin))

    val polarFbmSimplexAlt = fbmFunc2D(noise = ::simplex).withPolarInput()

    val polarFbm = simplex2D.fbm().withPolarInput()

}
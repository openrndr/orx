package org.openrndr.extra.color.colormaps

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

/**
 * Polynomial approximation in GLSL for the Turbo colormap.
 *
 * @see turboColormapVector
 * @see ColormapPhraseBook.turboColormap
 */
fun turboColormap(
    x: Double
): ColorRGBa = ColorRGBa.fromVector(
    turboColormapVector(x)
)

/**
 * Polynomial approximation in GLSL for the Turbo colormap.
 *
 * @see ColormapPhraseBook.turboColormap
 */
fun turboColormapVector(x: Double): Vector3 {
    val v = saturate(x)
    val v4 = Vector4( 1.0, v, v * v, v * v * v)
    val v2 = Vector2(v4.z, v4.w) * v4.z
    return Vector3(
        v4.dot(kRedVec4) + v2.dot(kRedVec2),
        v4.dot(kGreenVec4) + v2.dot(kGreenVec2),
        v4.dot(kBlueVec4) + v2.dot(kBlueVec2)
    )
}

private val kRedVec4 = Vector4(0.13572138, 4.61539260, -42.66032258, 132.13108234)
private val kGreenVec4 = Vector4(0.09140261, 2.19418839, 4.84296658, -14.18503333)
private val kBlueVec4 = Vector4(0.10667330, 12.64194608, -60.58204836, 110.36276771)
private val kRedVec2 = Vector2(-152.94239396, 59.28637943)
private val kGreenVec2 = Vector2(4.27729857, 2.82956604)
private val kBlueVec2 = Vector2(-89.90310912, 27.34824973)

package org.openrndr.extra.color.mixing

import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXYZa
import org.openrndr.extra.color.tools.matchLinearity

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/*
Direct port of https://github.com/rvanwijnen/spectral.js
 */

/**
 * Represents spectral power distribution (SPD) coefficients for the cyan channel,
 * spanning 38 wavelength samples. This array is used as part of the spectral upsampling
 * process to convert a linear RGB color into a reflectance spectrum.
 *
 * The coefficients are predefined values that define the contribution of the cyan channel
 * at specific wavelengths to the resulting reflectance spectrum.
 */
private val SPD_C = doubleArrayOf(
    0.96853629,
    0.96855103,
    0.96859338,
    0.96877345,
    0.96942204,
    0.97143709,
    0.97541862,
    0.98074186,
    0.98580992,
    0.98971194,
    0.99238027,
    0.99409844,
    0.995172,
    0.99576545,
    0.99593552,
    0.99564041,
    0.99464769,
    0.99229579,
    0.98638762,
    0.96829712,
    0.89228016,
    0.53740239,
    0.15360445,
    0.05705719,
    0.03126539,
    0.02205445,
    0.01802271,
    0.0161346,
    0.01520947,
    0.01475977,
    0.01454263,
    0.01444459,
    0.01439897,
    0.0143762,
    0.01436343,
    0.01435687,
    0.0143537,
    0.01435408
)

/**
 * Represents the spectral power distribution (SPD) data for the magenta color component.
 * This array contains 38 predefined values corresponding to specific wavelengths.
 * Used in the process of spectral upsampling to compute reflectance spectra from RGB color information.
 */
private val SPD_M = doubleArrayOf(
    0.51567122,
    0.5401552,
    0.62645502,
    0.75595012,
    0.92826996,
    0.97223624,
    0.98616174,
    0.98955255,
    0.98676237,
    0.97312575,
    0.91944277,
    0.32564851,
    0.13820628,
    0.05015143,
    0.02912336,
    0.02421691,
    0.02660696,
    0.03407586,
    0.04835936,
    0.0001172,
    0.00008554,
    0.85267882,
    0.93188793,
    0.94810268,
    0.94200977,
    0.91478045,
    0.87065445,
    0.78827548,
    0.65738359,
    0.59909403,
    0.56817268,
    0.54031997,
    0.52110241,
    0.51041094,
    0.50526577,
    0.5025508,
    0.50126452,
    0.50083021
)

/**
 * Spectral power distribution (SPD) values for the Yellow primary in the spectral upsampling process.
 * This array contains 38 precomputed reflectance values corresponding to specific wavelength samples.
 * It is used to calculate the reflectance spectrum when converting a linear RGB color to its spectral representation.
 */
private val SPD_Y = doubleArrayOf(
    0.02055257,
    0.02059936,
    0.02062723,
    0.02073387,
    0.02114202,
    0.02233154,
    0.02556857,
    0.03330189,
    0.05185294,
    0.10087639,
    0.24000413,
    0.53589066,
    0.79874659,
    0.91186529,
    0.95399623,
    0.97137099,
    0.97939505,
    0.98345207,
    0.98553736,
    0.98648905,
    0.98674535,
    0.98657555,
    0.98611877,
    0.98559942,
    0.98507063,
    0.98460039,
    0.98425301,
    0.98403909,
    0.98388535,
    0.98376116,
    0.98368246,
    0.98365023,
    0.98361309,
    0.98357259,
    0.98353856,
    0.98351247,
    0.98350101,
    0.98350852
)

/**
 * A predefined spectral power distribution (SPD) array for the red channel,
 * used in the spectral upsampling process to convert linear RGB colors into reflectance spectra.
 *
 * This array contains 38 values corresponding to specific wavelengths and represents
 * the relative spectral contribution of the red channel in the conversion process.
 *
 * The SPD_R array is utilized in conjunction with other SPDs (e.g., SPD_C, SPD_M, SPD_Y, SPD_G, SPD_B)
 * and the weights derived from spectral upsampling to calculate the reflectance spectrum
 * for a given color.
 */
private val SPD_R = doubleArrayOf(
    0.03147571,
    0.03146636,
    0.03140624,
    0.03119611,
    0.03053888,
    0.02856855,
    0.02459485,
    0.0192952,
    0.01423112,
    0.01033111,
    0.00765876,
    0.00593693,
    0.00485616,
    0.00426186,
    0.00409039,
    0.00438375,
    0.00537525,
    0.00772962,
    0.0136612,
    0.03181352,
    0.10791525,
    0.46249516,
    0.84604333,
    0.94275572,
    0.96860996,
    0.97783966,
    0.98187757,
    0.98377315,
    0.98470202,
    0.98515481,
    0.98537114,
    0.98546685,
    0.98550011,
    0.98551031,
    0.98550741,
    0.98551323,
    0.98551563,
    0.98551547
)

/**
 * Represents the predefined spectral power distribution (SPD) values for the green component
 * used in spectral upsampling of linear RGB colors.
 *
 * This array contains 38 reflectance values corresponding to specific wavelengths and
 * reflects the spectral characteristics of the green primary in the color model.
 *
 * It is utilized as one of the SPD datasets in the linearToReflectance function, which
 * converts linear RGB colors into reflectance spectra.
 */
private val SPD_G = doubleArrayOf(
    0.49108579,
    0.46944057,
    0.4016578,
    0.2449042,
    0.0682688,
    0.02732883,
    0.013606,
    0.01000187,
    0.01284127,
    0.02636635,
    0.07058713,
    0.70421692,
    0.85473994,
    0.95081565,
    0.9717037,
    0.97651888,
    0.97429245,
    0.97012917,
    0.9425863,
    0.99989207,
    0.99989891,
    0.13823139,
    0.06968113,
    0.05628787,
    0.06111561,
    0.08987709,
    0.13656016,
    0.22169624,
    0.32176956,
    0.36157329,
    0.4836192,
    0.46488579,
    0.47440306,
    0.4857699,
    0.49267971,
    0.49625685,
    0.49807754,
    0.49889859
)

/**
 * Represents the spectral power distribution (SPD) values corresponding to the blue component
 * of a linear RGB color. The array contains 38 precomputed reflectance values
 * that span a specific range of wavelengths. These values are utilized in the spectral
 * upsampling process to map an RGB color to its equivalent spectral reflectance distribution.
 */
private val SPD_B = doubleArrayOf(
    0.97901834,
    0.97901649,
    0.97901118,
    0.97892146,
    0.97858555,
    0.97743705,
    0.97428075,
    0.96663223,
    0.94822893,
    0.89937713,
    0.76070164,
    0.4642044,
    0.20123039,
    0.08808402,
    0.04592894,
    0.02860373,
    0.02060067,
    0.01656701,
    0.01451549,
    0.01357964,
    0.01331243,
    0.01347661,
    0.01387181,
    0.01435472,
    0.01479836,
    0.0151525,
    0.01540513,
    0.01557233,
    0.0156571,
    0.01571025,
    0.01571916,
    0.01572133,
    0.01572502,
    0.01571717,
    0.01571905,
    0.01571059,
    0.01569728,
    0.0157002
)

/**
 * A pre-defined array representing the CIE 1931 Standard Observer's color matching function values for the X component.
 * This data is used in color science calculations to transform spectral reflectance data into the CIE XYZ color space.
 * The array contains 38 discrete samples corresponding to wavelengths within the visible spectrum.
 *
 * This constant is specifically used during computations involving spectral data to calculate the X component
 * of the CIE XYZ color space via multiplication with a corresponding reflectance spectrum array.
 */
private val CIE_CMF_X = doubleArrayOf(
    0.00006469,
    0.00021941,
    0.00112057,
    0.00376661,
    0.01188055,
    0.02328644,
    0.03455942,
    0.03722379,
    0.03241838,
    0.02123321,
    0.01049099,
    0.00329584,
    0.00050704,
    0.00094867,
    0.00627372,
    0.01686462,
    0.02868965,
    0.04267481,
    0.05625475,
    0.0694704,
    0.08305315,
    0.0861261,
    0.09046614,
    0.08500387,
    0.07090667,
    0.05062889,
    0.03547396,
    0.02146821,
    0.01251646,
    0.00680458,
    0.00346457,
    0.00149761,
    0.0007697,
    0.00040737,
    0.00016901,
    0.00009522,
    0.00004903,
    0.00002
)

/**
 * Represents the Y-component of the CIE 1931 color matching functions.
 *
 * The CIE 1931 color matching functions are used to convert spectral power distributions into
 * CIE XYZ tristimulus values, which represent a color in a perceptually-uniform color space.
 * These functions are defined over 38 discrete wavelength samples, typically covering
 * the visible spectrum.
 *
 * The Y-component corresponds to the luminous efficiency function,
 * which describes the sensitivity of human vision to different wavelengths of light.
 */
private val CIE_CMF_Y = doubleArrayOf(
    0.00000184,
    0.00000621,
    0.00003101,
    0.00010475,
    0.00035364,
    0.00095147,
    0.00228226,
    0.00420733,
    0.0066888,
    0.0098884,
    0.01524945,
    0.02141831,
    0.03342293,
    0.05131001,
    0.07040208,
    0.08783871,
    0.09424905,
    0.09795667,
    0.09415219,
    0.08678102,
    0.07885653,
    0.0635267,
    0.05374142,
    0.04264606,
    0.03161735,
    0.02088521,
    0.01386011,
    0.00810264,
    0.0046301,
    0.00249138,
    0.0012593,
    0.00054165,
    0.00027795,
    0.00014711,
    0.00006103,
    0.00003439,
    0.00001771,
    0.00000722
)

/**
 * Represents the Z component of the CIE 1931 2° Standard Observer Color Matching Function (CMF).
 *
 * This array contains precomputed values representing the spectral sensitivity of the human eye's Z cone.
 * It is used in color science to convert spectral reflectance data into the Z component of the CIE XYZ color space.
 *
 * The values in this array correspond to sampling points across the visible light spectrum
 * and are used in conjunction with `CIE_CMF_X` and `CIE_CMF_Y` to perform reflectance spectrum to XYZ color conversions.
 */
private val CIE_CMF_Z = doubleArrayOf(
    0.00030502,
    0.00103681,
    0.00531314,
    0.01795439,
    0.05707758,
    0.11365162,
    0.17335873,
    0.19620658,
    0.18608237,
    0.13995048,
    0.08917453,
    0.04789621,
    0.02814563,
    0.01613766,
    0.0077591,
    0.00429615,
    0.00200551,
    0.00086147,
    0.00036904,
    0.00019143,
    0.00014956,
    0.00009231,
    0.00006813,
    0.00002883,
    0.00001577,
    0.00000394,
    0.00000158,
    0.0,
    0.0,
    0.0,
    0.0,
    0.0,
    0.0,
    0.0,
    0.0,
    0.0,
    0.0,
    0.0
)

/**
 * Converts RGB color values into a form that represents spectral power distribution weights.
 *
 * @param rgb The RGB color in the form of a `ColorRGBa` object, which is to be converted to spectral weights.
 * @return A `DoubleArray` representing the decomposed weights for white, cyan, magenta, yellow, red, green, and blue.
 */
private fun spectralUpsampling(rgb: ColorRGBa): DoubleArray {
    var lrgb = rgb.toLinear()
    val w = min(min(lrgb.r, lrgb.g), lrgb.b)

    lrgb = lrgb.copy(r = lrgb.r - w, g = lrgb.g - w, lrgb.b - w)

    val c = max(0.0, min(lrgb.g, lrgb.b))
    val m = max(0.0, min(lrgb.r, lrgb.b))
    val y = max(0.0, min(lrgb.r, lrgb.g))
    val r = max(0.0, min(lrgb.r - lrgb.b, lrgb.r - lrgb.g))
    val g = max(0.0, min(lrgb.g - lrgb.b, lrgb.g - lrgb.r))
    val b = max(0.0, min(lrgb.b - lrgb.g, lrgb.b - lrgb.r))

    return doubleArrayOf(max(0.0, w), c, m, y, r, g, b)
}


/**
 * Converts a linear RGB color into a reflectance spectrum represented as a `DoubleArray`.
 * The resulting reflectance spectrum spans 38 wavelength samples
 * and is computed using spectral upsampling based on predefined spectral distributions.
 *
 * @param rgb The linear RGB color to be converted, represented as a `ColorRGBa` object.
 * @return A `DoubleArray` containing 38 reflectance values corresponding to the wavelengths.
 */
internal fun linearToReflectance(rgb: ColorRGBa): DoubleArray {
    val eps = 0.00000001
    val weights = spectralUpsampling(rgb)
    val reflectance = DoubleArray(38)

    for (i in 0 until 38) {
        reflectance[i] = max(
            eps,
            weights[0] +
                    weights[1] * SPD_C[i] +
                    weights[2] * SPD_M[i] +
                    weights[3] * SPD_Y[i] +
                    weights[4] * SPD_R[i] +
                    weights[5] * SPD_G[i] +
                    weights[6] * SPD_B[i]
        )
    }
    return reflectance
}

/**
 * Computes the concentration of a component in a linear interpolation based on the specified parameters.
 *
 * The method calculates a concentration factor `c` using two linear values `l1` and `l2`,
 * as well as a blend factor `t`. This can be used in scenarios such as spectral mixing
 * or interpolation tasks where weights are dynamically computed.
 *
 * @param l1 The first linear value, typically derived from reflectance or intensity.
 * @param l2 The second linear value, typically derived from reflectance or intensity.
 * @param t The blending factor in the range [0.0, 1.0], where 0.0 represents full influence of `l1`
 *          and 1.0 represents full influence of `l2`.
 * @return The computed concentration factor as a `Double`, representing the relative contribution
 *         of the components based on the blending factor.
 */
private fun linearToConcentration(l1: Double, l2: Double, t: Double): Double {
    val t1 = l1 * (1 - t).pow(2.0)
    val t2 = l2 * t.pow(2.0)

    return t2 / (t1 + t2)
}

private fun DoubleArray.dot(other: DoubleArray): Double {
    var d = 0.0
    for (i in indices) {
        d += this[i] * other[i]
    }
    return d
}

/**
 * Converts a reflectance spectrum represented as a `DoubleArray` to a color in the CIE XYZ color space.
 *
 * This method calculates the XYZ color by performing a dot product operation between the reflectance spectrum
 * and the CIE color matching functions (CIE_CMF_X, CIE_CMF_Y, CIE_CMF_Z). The result is returned as a `ColorXYZa` object.
 *
 * @param reflectance An array of reflectance spectrum values, typically spanning the visible spectrum.
 *                    This is represented as a `DoubleArray` with 38 wavelength samples.
 * @return A `ColorXYZa` object representing the corresponding color in the CIE XYZ color space.
 */
internal fun reflectanceToXYZ(reflectance: DoubleArray): ColorXYZa {
    val x = reflectance.dot(CIE_CMF_X)
    val y = reflectance.dot(CIE_CMF_Y)
    val z = reflectance.dot(CIE_CMF_Z)
    return ColorXYZa(x, y, z)
}

private fun pow(x: Double, y: Double): Double = x.pow(y)

/**
 * Applies the Saunderson correction to a reflectance value to account for surface reflectance effects.
 *
 * Saunderson correction adjusts the measured reflectance by considering front surface reflection
 * and internal reflections in the material. The correction is based on two coefficients, `k1` and `k2`.
 *
 * @param rInf The measured reflectance value to be corrected.
 * @param k1 The first Saunderson coefficient representing front surface reflection.
 * @param k2 The second Saunderson coefficient representing internal reflection effects.
 * @return The corrected reflectance value as a `Double`.
 */
internal fun saundersonCorrection(rInf: Double, k1: Double, k2: Double): Double {
    return k1 + ((1 - k1) * (1 - k2) * rInf) / (1 - (k2 * rInf))
}

/**
 * Blends two colors spectrally by interpolating their reflectance spectra and returns the resulting color.
 * This method uses spectral upsampling, Saunderson correction, and concentration factors to compute
 * the resulting color in the RGB color space.
 *
 * @param color1 The first color to be mixed, represented as a `ColorRGBa`.
 * @param color2 The second color to be mixed, represented as a `ColorRGBa`.
 * @param t The blending factor in the range [0.0, 1.0], where 0.0 represents full influence of `color1`
 *          and 1.0 represents full influence of `color2`.
 * @param k1 The first Saunderson correction coefficient for surface reflection. Default is 0.0.
 * @param k2 The second Saunderson correction coefficient for internal reflections. Default is 0.0.
 * @return The resulting blended color as a `ColorRGBa`, maintaining the linearity of the first input color (`color1`).
 */
fun mixSpectral(color1: ColorRGBa, color2: ColorRGBa, t: Double, k1: Double = 0.0, k2: Double = 0.0): ColorRGBa {
    val lrgb1 = color1.toLinear()
    val lrgb2 = color2.toLinear()

    val ref1 = linearToReflectance(lrgb1)
    val ref2 = linearToReflectance(lrgb2)

    val l1 = ref1.dot(CIE_CMF_Y)
    val l2 = ref2.dot(CIE_CMF_Y)

    val c = linearToConcentration(l1, l2, t)

    val reflectance = DoubleArray(38)

    for (i in 0 until 38) {
        val ks =
            (1.0 - c) * (pow(1.0 - ref1[i], 2.0) / (2.0 * ref1[i])) + c * (pow(
                1.0 - ref2[i],
                2.0
            ) / (2.0 * ref2[i]))
        val km = (1.0 + ks - sqrt(pow(ks, 2.0) + 2.0 * ks))

        reflectance[i] = saundersonCorrection(km, k1, k2)
    }

    val xyz = reflectanceToXYZ(reflectance)
    return xyz.toRGBa().matchLinearity(color1)
}
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
fun linearToReflectance(rgb: ColorRGBa): DoubleArray {
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
fun reflectanceToXYZ(reflectance: DoubleArray): ColorXYZa {
    val x = reflectance.dot(CIE_CMF_X)
    val y = reflectance.dot(CIE_CMF_Y)
    val z = reflectance.dot(CIE_CMF_Z)
    return ColorXYZa(x, y, z)
}

private fun pow(x: Double, y: Double): Double = x.pow(y)

/**
 * Applies the Saunderson correction: converts the internal reflectance of a paint layer, as computed by
 * Kubelka–Munk theory, to the reflectance seen from outside, which includes the reflections at the surface.
 *
 * @param rInf The internal (Kubelka–Munk) reflectance.
 * @param k1 The fraction of incident light reflected at the front surface, about 0.04 for a refractive index of 1.5,
 * 0 when the specular reflection is excluded.
 * @param k2 The fraction of diffuse light inside the layer that is reflected back in at the surface, about 0.4 to 0.6.
 * @return The reflectance seen from outside.
 * @see inverseSaundersonCorrection
 */
internal fun saundersonCorrection(rInf: Double, k1: Double, k2: Double): Double {
    return k1 + ((1 - k1) * (1 - k2) * rInf) / (1 - (k2 * rInf))
}

/**
 * Inverts the Saunderson correction: converts the reflectance seen from outside to the internal reflectance of the
 * paint layer, which is what Kubelka–Munk theory works with. Reflectances below what the front surface alone
 * reflects ([k1]) have no internal equivalent and are clamped.
 *
 * @param rM The reflectance seen from outside.
 * @param k1 The fraction of incident light reflected at the front surface.
 * @param k2 The fraction of diffuse light inside the layer that is reflected back in at the surface.
 * @return The internal reflectance.
 * @see saundersonCorrection
 */
internal fun inverseSaundersonCorrection(rM: Double, k1: Double, k2: Double): Double {
    return ((rM - k1) / (1 - k1 - k2 + k2 * rM)).coerceAtLeast(0.00000001)
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
 * @param k1 The Saunderson coefficient for reflection at the front surface, about 0.04 for glossy paint. Default is
 *           0.0.
 * @param k2 The Saunderson coefficient for internal reflection at the surface, about 0.4 to 0.6. Default is 0.0.
 *           With k1 = k2 = 0 no surface reflections are modeled. Otherwise the colors are treated as seen from
 *           outside: their reflectances are converted to internal reflectances before mixing and back after mixing,
 *           so mixing a color with itself returns that color.
 * @return The resulting blended color as a `ColorRGBa`, maintaining the linearity of the first input color (`color1`).
 */
fun mixSpectral(color1: ColorRGBa, color2: ColorRGBa, t: Double, k1: Double = 0.0, k2: Double = 0.0): ColorRGBa {
    val lrgb1 = color1.toLinear()
    val lrgb2 = color2.toLinear()

    val ref1 = linearToReflectance(lrgb1)
    val ref2 = linearToReflectance(lrgb2)

    val reflectance = mixReflectance(ref1, ref2, t, k1, k2)

    val xyz = reflectanceToXYZ(reflectance)
    return xyz.toRGBa().matchLinearity(color1)
}

/**
 * Mixes two reflectance spectra with Kubelka–Munk theory, like [mixSpectral] but without converting from and to RGB.
 * Use it with measured reflectances such as [ColorCheckerReflectances].
 *
 * @param reflectance1 The first reflectance spectrum, 38 samples from 380 to 750 nm in steps of 10 nm.
 * @param reflectance2 The second reflectance spectrum.
 * @param t The blending factor in the range [0.0, 1.0], where 0.0 is `reflectance1` and 1.0 is `reflectance2`.
 * @param k1 The Saunderson coefficient for reflection at the front surface, see [mixSpectral].
 * @param k2 The Saunderson coefficient for internal reflection at the surface, see [mixSpectral].
 * @return The reflectance spectrum of the mix.
 */
fun mixReflectance(
    reflectance1: DoubleArray,
    reflectance2: DoubleArray,
    t: Double,
    k1: Double = 0.0,
    k2: Double = 0.0
): DoubleArray {
    require(reflectance1.size == 38 && reflectance2.size == 38) { "reflectances must have 38 samples" }
    val l1 = reflectance1.dot(CIE_CMF_Y)
    val l2 = reflectance2.dot(CIE_CMF_Y)

    val c = linearToConcentration(l1, l2, t)

    val reflectance = DoubleArray(38)

    for (i in 0 until 38) {
        // Kubelka–Munk works with the internal reflectance of the layer, without the reflections at its surface,
        // reflectances are kept above 0 so that K/S stays finite
        val r1 = inverseSaundersonCorrection(max(reflectance1[i], 0.00000001), k1, k2)
        val r2 = inverseSaundersonCorrection(max(reflectance2[i], 0.00000001), k1, k2)
        val ks = (1.0 - c) * (pow(1.0 - r1, 2.0) / (2.0 * r1)) + c * (pow(1.0 - r2, 2.0) / (2.0 * r2))
        val km = (1.0 + ks - sqrt(pow(ks, 2.0) + 2.0 * ks))

        reflectance[i] = saundersonCorrection(km, k1, k2)
    }
    return reflectance
}

/**
 * Converts a reflectance spectrum to the color of the surface under D65 light, in linear RGB.
 *
 * @param reflectance A reflectance spectrum with 38 samples from 380 to 750 nm in steps of 10 nm.
 */
fun reflectanceToRGBa(reflectance: DoubleArray): ColorRGBa = reflectanceToXYZ(reflectance).toRGBa()

/**
 * Measured reflectance spectra of the 24 patches of the X-Rite (formerly Macbeth) ColorChecker Classic, for use with
 * [mixReflectance] and [reflectanceToRGBa].
 *
 * Every spectrum has 38 samples from 380 to 750 nm in steps of 10 nm, the grid used by [linearToReflectance] and
 * [reflectanceToXYZ]. The data was measured by N. Ohta, *The basis of color reproduction engineering* (1997), and
 * is identical to the data in ISO 17321-1. It was taken from colour-science
 * (https://github.com/colour-science/colour, BSD-3-Clause), resampled from 5 nm to 10 nm by taking every other
 * sample. Every property returns a new copy of the spectrum.
 */
object ColorCheckerReflectances {
    private val DARK_SKIN = doubleArrayOf(
        0.048, 0.055, 0.065, 0.068, 0.064, 0.059, 0.055, 0.053, 0.052, 0.052, 0.054, 0.057, 0.061, 0.065, 0.07,
        0.074, 0.076, 0.079, 0.087, 0.1, 0.115, 0.129, 0.138, 0.146, 0.154, 0.163, 0.173, 0.188, 0.204, 0.222,
        0.242, 0.261, 0.282, 0.305, 0.334, 0.372, 0.409, 0.436
    )
    private val LIGHT_SKIN = doubleArrayOf(
        0.103, 0.141, 0.182, 0.197, 0.201, 0.205, 0.212, 0.224, 0.24, 0.262, 0.282, 0.293, 0.301, 0.321, 0.322,
        0.298, 0.292, 0.3, 0.295, 0.305, 0.358, 0.435, 0.494, 0.53, 0.55, 0.564, 0.574, 0.59, 0.605, 0.624, 0.652,
        0.682, 0.713, 0.745, 0.762, 0.783, 0.791, 0.789
    )
    private val BLUE_SKY = doubleArrayOf(
        0.113, 0.174, 0.266, 0.32, 0.336, 0.337, 0.335, 0.331, 0.322, 0.31, 0.293, 0.276, 0.26, 0.243, 0.225,
        0.208, 0.198, 0.191, 0.183, 0.172, 0.163, 0.157, 0.15, 0.144, 0.137, 0.13, 0.123, 0.118, 0.112, 0.108,
        0.105, 0.104, 0.103, 0.102, 0.102, 0.102, 0.104, 0.104
    )
    private val FOLIAGE = doubleArrayOf(
        0.048, 0.049, 0.05, 0.049, 0.05, 0.052, 0.054, 0.058, 0.061, 0.064, 0.067, 0.07, 0.078, 0.106, 0.155,
        0.181, 0.177, 0.157, 0.137, 0.126, 0.122, 0.115, 0.104, 0.098, 0.098, 0.1, 0.097, 0.095, 0.095, 0.101,
        0.125, 0.174, 0.247, 0.311, 0.343, 0.358, 0.364, 0.362
    )
    private val BLUE_FLOWER = doubleArrayOf(
        0.123, 0.197, 0.328, 0.418, 0.446, 0.448, 0.444, 0.434, 0.421, 0.405, 0.381, 0.362, 0.342, 0.314, 0.271,
        0.231, 0.211, 0.209, 0.201, 0.196, 0.206, 0.223, 0.235, 0.245, 0.243, 0.247, 0.269, 0.318, 0.384, 0.446,
        0.49, 0.511, 0.52, 0.523, 0.521, 0.522, 0.521, 0.514
    )
    private val BLUISH_GREEN = doubleArrayOf(
        0.11, 0.167, 0.252, 0.303, 0.322, 0.336, 0.353, 0.375, 0.408, 0.46, 0.523, 0.566, 0.582, 0.58, 0.569,
        0.549, 0.519, 0.48, 0.436, 0.392, 0.346, 0.302, 0.26, 0.234, 0.221, 0.215, 0.21, 0.208, 0.211, 0.22, 0.233,
        0.244, 0.252, 0.25, 0.244, 0.245, 0.26, 0.278
    )
    private val ORANGE = doubleArrayOf(
        0.053, 0.054, 0.054, 0.053, 0.052, 0.052, 0.052, 0.052, 0.052, 0.053, 0.055, 0.057, 0.061, 0.068, 0.086,
        0.12, 0.17, 0.228, 0.297, 0.38, 0.452, 0.503, 0.532, 0.552, 0.566, 0.578, 0.587, 0.599, 0.604, 0.608,
        0.615, 0.622, 0.628, 0.633, 0.633, 0.637, 0.638, 0.633
    )
    private val PURPLISH_BLUE = doubleArrayOf(
        0.099, 0.15, 0.231, 0.293, 0.324, 0.348, 0.373, 0.387, 0.374, 0.345, 0.301, 0.247, 0.202, 0.167, 0.137,
        0.116, 0.106, 0.099, 0.09, 0.083, 0.083, 0.086, 0.087, 0.085, 0.084, 0.088, 0.098, 0.111, 0.123, 0.126,
        0.12, 0.115, 0.116, 0.12, 0.128, 0.139, 0.162, 0.197
    )
    private val MODERATE_RED = doubleArrayOf(
        0.096, 0.123, 0.144, 0.144, 0.138, 0.132, 0.131, 0.129, 0.126, 0.125, 0.119, 0.109, 0.103, 0.1, 0.094,
        0.089, 0.092, 0.102, 0.108, 0.112, 0.157, 0.274, 0.415, 0.517, 0.567, 0.591, 0.601, 0.607, 0.607, 0.605,
        0.605, 0.605, 0.606, 0.602, 0.599, 0.596, 0.593, 0.584
    )
    private val PURPLE = doubleArrayOf(
        0.101, 0.135, 0.177, 0.199, 0.206, 0.19, 0.168, 0.144, 0.12, 0.101, 0.086, 0.075, 0.067, 0.061, 0.058,
        0.054, 0.052, 0.053, 0.055, 0.054, 0.052, 0.053, 0.059, 0.074, 0.099, 0.126, 0.149, 0.172, 0.193, 0.217,
        0.248, 0.282, 0.319, 0.355, 0.388, 0.422, 0.451, 0.471
    )
    private val YELLOW_GREEN = doubleArrayOf(
        0.056, 0.059, 0.06, 0.061, 0.062, 0.064, 0.068, 0.075, 0.085, 0.104, 0.135, 0.185, 0.269, 0.384, 0.484,
        0.534, 0.545, 0.533, 0.513, 0.487, 0.454, 0.416, 0.374, 0.346, 0.331, 0.325, 0.32, 0.319, 0.324, 0.337,
        0.354, 0.368, 0.379, 0.379, 0.373, 0.375, 0.392, 0.412
    )
    private val ORANGE_YELLOW = doubleArrayOf(
        0.06, 0.063, 0.065, 0.064, 0.064, 0.064, 0.065, 0.067, 0.069, 0.077, 0.092, 0.107, 0.123, 0.146, 0.193,
        0.273, 0.374, 0.456, 0.512, 0.554, 0.584, 0.609, 0.624, 0.635, 0.645, 0.654, 0.662, 0.672, 0.676, 0.678,
        0.685, 0.69, 0.696, 0.698, 0.698, 0.701, 0.701, 0.694
    )
    private val BLUE = doubleArrayOf(
        0.069, 0.096, 0.136, 0.175, 0.208, 0.244, 0.29, 0.335, 0.338, 0.302, 0.239, 0.172, 0.12, 0.086, 0.066,
        0.054, 0.048, 0.045, 0.043, 0.041, 0.04, 0.04, 0.039, 0.04, 0.04, 0.041, 0.042, 0.042, 0.043, 0.044, 0.044,
        0.045, 0.048, 0.051, 0.056, 0.064, 0.079, 0.104
    )
    private val GREEN = doubleArrayOf(
        0.055, 0.057, 0.058, 0.059, 0.059, 0.062, 0.065, 0.07, 0.078, 0.091, 0.113, 0.14, 0.18, 0.244, 0.324,
        0.363, 0.355, 0.323, 0.281, 0.238, 0.196, 0.158, 0.124, 0.101, 0.089, 0.084, 0.08, 0.077, 0.075, 0.075,
        0.078, 0.082, 0.088, 0.089, 0.09, 0.089, 0.094, 0.102
    )
    private val RED = doubleArrayOf(
        0.052, 0.052, 0.051, 0.05, 0.049, 0.049, 0.049, 0.049, 0.048, 0.047, 0.045, 0.044, 0.044, 0.044, 0.044,
        0.044, 0.045, 0.047, 0.05, 0.057, 0.072, 0.109, 0.192, 0.332, 0.486, 0.598, 0.654, 0.686, 0.7, 0.707,
        0.718, 0.724, 0.729, 0.73, 0.727, 0.729, 0.727, 0.721
    )
    private val YELLOW = doubleArrayOf(
        0.054, 0.054, 0.053, 0.053, 0.052, 0.053, 0.053, 0.055, 0.059, 0.075, 0.121, 0.202, 0.303, 0.394, 0.475,
        0.544, 0.597, 0.63, 0.66, 0.686, 0.708, 0.726, 0.737, 0.746, 0.753, 0.761, 0.768, 0.777, 0.78, 0.781,
        0.785, 0.787, 0.792, 0.793, 0.79, 0.792, 0.787, 0.778
    )
    private val MAGENTA = doubleArrayOf(
        0.118, 0.179, 0.283, 0.343, 0.359, 0.35, 0.327, 0.298, 0.267, 0.239, 0.209, 0.182, 0.163, 0.146, 0.124,
        0.106, 0.102, 0.107, 0.106, 0.112, 0.141, 0.198, 0.279, 0.394, 0.522, 0.628, 0.696, 0.742, 0.766, 0.78,
        0.791, 0.798, 0.804, 0.807, 0.807, 0.813, 0.813, 0.808
    )
    private val CYAN = doubleArrayOf(
        0.093, 0.134, 0.195, 0.238, 0.258, 0.281, 0.315, 0.352, 0.391, 0.434, 0.458, 0.457, 0.433, 0.392, 0.339,
        0.282, 0.228, 0.18, 0.141, 0.114, 0.097, 0.088, 0.08, 0.075, 0.073, 0.073, 0.073, 0.074, 0.076, 0.077,
        0.075, 0.074, 0.072, 0.071, 0.075, 0.082, 0.1, 0.133
    )
    private val WHITE = doubleArrayOf(
        0.153, 0.245, 0.409, 0.671, 0.84, 0.878, 0.883, 0.886, 0.887, 0.888, 0.888, 0.888, 0.887, 0.887, 0.887,
        0.887, 0.887, 0.886, 0.887, 0.888, 0.887, 0.886, 0.887, 0.889, 0.891, 0.891, 0.89, 0.889, 0.889, 0.888,
        0.888, 0.888, 0.888, 0.886, 0.886, 0.885, 0.884, 0.883
    )
    private val NEUTRAL8 = doubleArrayOf(
        0.15, 0.235, 0.372, 0.529, 0.58, 0.585, 0.587, 0.588, 0.586, 0.583, 0.581, 0.58, 0.58, 0.58, 0.581, 0.582,
        0.583, 0.583, 0.584, 0.586, 0.588, 0.588, 0.588, 0.586, 0.585, 0.583, 0.58, 0.578, 0.576, 0.574, 0.572,
        0.57, 0.568, 0.566, 0.564, 0.562, 0.56, 0.557
    )
    private val NEUTRAL65 = doubleArrayOf(
        0.138, 0.206, 0.289, 0.346, 0.357, 0.359, 0.361, 0.362, 0.361, 0.358, 0.357, 0.356, 0.356, 0.356, 0.357,
        0.357, 0.358, 0.358, 0.359, 0.36, 0.361, 0.361, 0.36, 0.359, 0.357, 0.355, 0.353, 0.351, 0.349, 0.346,
        0.345, 0.343, 0.341, 0.339, 0.337, 0.335, 0.333, 0.331
    )
    private val NEUTRAL5 = doubleArrayOf(
        0.113, 0.15, 0.183, 0.199, 0.202, 0.203, 0.205, 0.205, 0.204, 0.203, 0.202, 0.202, 0.202, 0.202, 0.202,
        0.203, 0.203, 0.203, 0.203, 0.204, 0.205, 0.205, 0.204, 0.204, 0.203, 0.201, 0.2, 0.198, 0.197, 0.196,
        0.195, 0.194, 0.192, 0.191, 0.19, 0.189, 0.188, 0.187
    )
    private val NEUTRAL35 = doubleArrayOf(
        0.074, 0.084, 0.091, 0.094, 0.094, 0.094, 0.095, 0.095, 0.094, 0.094, 0.093, 0.093, 0.092, 0.093, 0.093,
        0.093, 0.093, 0.093, 0.093, 0.093, 0.093, 0.093, 0.092, 0.092, 0.091, 0.09, 0.09, 0.089, 0.089, 0.088,
        0.087, 0.087, 0.086, 0.086, 0.085, 0.085, 0.085, 0.084
    )
    private val BLACK = doubleArrayOf(
        0.032, 0.033, 0.035, 0.036, 0.036, 0.036, 0.035, 0.035, 0.035, 0.035, 0.034, 0.034, 0.034, 0.034, 0.034,
        0.034, 0.034, 0.034, 0.033, 0.033, 0.033, 0.033, 0.033, 0.033, 0.033, 0.033, 0.033, 0.033, 0.033, 0.033,
        0.033, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032, 0.032
    )

    /** ColorChecker patch "dark skin" */
    val darkSkin: DoubleArray get() = DARK_SKIN.copyOf()

    /** ColorChecker patch "light skin" */
    val lightSkin: DoubleArray get() = LIGHT_SKIN.copyOf()

    /** ColorChecker patch "blue sky" */
    val blueSky: DoubleArray get() = BLUE_SKY.copyOf()

    /** ColorChecker patch "foliage" */
    val foliage: DoubleArray get() = FOLIAGE.copyOf()

    /** ColorChecker patch "blue flower" */
    val blueFlower: DoubleArray get() = BLUE_FLOWER.copyOf()

    /** ColorChecker patch "bluish green" */
    val bluishGreen: DoubleArray get() = BLUISH_GREEN.copyOf()

    /** ColorChecker patch "orange" */
    val orange: DoubleArray get() = ORANGE.copyOf()

    /** ColorChecker patch "purplish blue" */
    val purplishBlue: DoubleArray get() = PURPLISH_BLUE.copyOf()

    /** ColorChecker patch "moderate red" */
    val moderateRed: DoubleArray get() = MODERATE_RED.copyOf()

    /** ColorChecker patch "purple" */
    val purple: DoubleArray get() = PURPLE.copyOf()

    /** ColorChecker patch "yellow green" */
    val yellowGreen: DoubleArray get() = YELLOW_GREEN.copyOf()

    /** ColorChecker patch "orange yellow" */
    val orangeYellow: DoubleArray get() = ORANGE_YELLOW.copyOf()

    /** ColorChecker patch "blue" */
    val blue: DoubleArray get() = BLUE.copyOf()

    /** ColorChecker patch "green" */
    val green: DoubleArray get() = GREEN.copyOf()

    /** ColorChecker patch "red" */
    val red: DoubleArray get() = RED.copyOf()

    /** ColorChecker patch "yellow" */
    val yellow: DoubleArray get() = YELLOW.copyOf()

    /** ColorChecker patch "magenta" */
    val magenta: DoubleArray get() = MAGENTA.copyOf()

    /** ColorChecker patch "cyan" */
    val cyan: DoubleArray get() = CYAN.copyOf()

    /** ColorChecker patch "white 9.5 (.05 D)" */
    val white: DoubleArray get() = WHITE.copyOf()

    /** ColorChecker patch "neutral 8 (.23 D)" */
    val neutral8: DoubleArray get() = NEUTRAL8.copyOf()

    /** ColorChecker patch "neutral 6.5 (.44 D)" */
    val neutral65: DoubleArray get() = NEUTRAL65.copyOf()

    /** ColorChecker patch "neutral 5 (.70 D)" */
    val neutral5: DoubleArray get() = NEUTRAL5.copyOf()

    /** ColorChecker patch "neutral 3.5 (1.05 D)" */
    val neutral35: DoubleArray get() = NEUTRAL35.copyOf()

    /** ColorChecker patch "black 2 (1.5 D)" */
    val black: DoubleArray get() = BLACK.copyOf()

    /**
     * All 24 patches by name, in chart order: row by row from the top left.
     */
    val all: Map<String, DoubleArray>
        get() = linkedMapOf(
            "dark skin" to darkSkin,
            "light skin" to lightSkin,
            "blue sky" to blueSky,
            "foliage" to foliage,
            "blue flower" to blueFlower,
            "bluish green" to bluishGreen,
            "orange" to orange,
            "purplish blue" to purplishBlue,
            "moderate red" to moderateRed,
            "purple" to purple,
            "yellow green" to yellowGreen,
            "orange yellow" to orangeYellow,
            "blue" to blue,
            "green" to green,
            "red" to red,
            "yellow" to yellow,
            "magenta" to magenta,
            "cyan" to cyan,
            "white 9.5 (.05 D)" to white,
            "neutral 8 (.23 D)" to neutral8,
            "neutral 6.5 (.44 D)" to neutral65,
            "neutral 5 (.70 D)" to neutral5,
            "neutral 3.5 (1.05 D)" to neutral35,
            "black 2 (1.5 D)" to black
        )
}

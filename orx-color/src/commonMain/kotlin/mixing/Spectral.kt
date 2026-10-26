package org.openrndr.extra.color.mixing

import org.openrndr.color.ColorRGBa
import org.openrndr.color.ColorXYZa
import org.openrndr.color.Linearity
import org.openrndr.extra.color.spaces.ColorOKLABa
import org.openrndr.extra.color.spaces.ColorOKLCHa
import org.openrndr.extra.color.tools.matchLinearity

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.jvm.JvmName

/*
Direct port of https://github.com/rvanwijnen/spectral.js
 */

/*
The reflectance basis spectra and the color matching functions are those of spectral.js 3.0.0. They replace the
spectral.js 2 data, whose magenta and green basis spectra had a discontinuity at 570 to 590 nm.
 */

/**
 * Reflectance basis spectrum for white, 38 samples from 380 to 750 nm in steps of 10 nm. Linear RGB colors are
 * decomposed into white, one secondary and one primary, see [linearToReflectance].
 */
private val BASE_W = doubleArrayOf(
    1.00116072718764, 1.00116065159728, 1.00116031922747, 1.00115867270789, 1.00115259844552, 1.00113252528998,
    1.00108500663327, 1.00099687889453, 1.00086525152274, 1.0006962900094, 1.00050496114888, 1.00030808187992,
    1.00011966602013, 0.999952765968407, 0.999821836899297, 0.999738609557593, 0.999709551639612,
    0.999731930210627, 0.999799436346195, 0.999900330316671, 1.00002040652611, 1.00014478793658, 1.00025997903412,
    1.00035579697089, 1.00042753780269, 1.00047623344888, 1.00050720967508, 1.00052519156373, 1.00053509606896,
    1.00054022097482, 1.00054272816784, 1.00054389569087, 1.00054448212151, 1.00054476959992, 1.00054489887762,
    1.00054496254689, 1.00054498927058, 1.000544996993
)

/**
 * Reflectance basis spectrum for cyan, 38 samples from 380 to 750 nm in steps of 10 nm. Linear RGB colors are
 * decomposed into white, one secondary and one primary, see [linearToReflectance].
 */
private val BASE_C = doubleArrayOf(
    0.970585001322962, 0.970592498143425, 0.970625348729891, 0.970786806119017, 0.971368673228248,
    0.973163230621252, 0.976740223158765, 0.981587605491377, 0.986280265652949, 0.989949147689134,
    0.99249270153842, 0.994145680405256, 0.995183975033212, 0.995756750110818, 0.99591281828671, 0.995606157834528,
    0.994597600961854, 0.99221571549237, 0.986236452783249, 0.967943337264541, 0.891285004244943,
    0.536202477862053, 0.154108119001878, 0.0574575093228929, 0.0315349873107007, 0.0222633920086335,
    0.0182022841492439, 0.016299055973264, 0.0153656239334613, 0.0149111568733976, 0.0146954339898235,
    0.0145964146717719, 0.0145470156699655, 0.0145228771899495, 0.0145120341118965, 0.0145066940939832,
    0.0145044507314479, 0.0145038009464639
)

/**
 * Reflectance basis spectrum for magenta, 38 samples from 380 to 750 nm in steps of 10 nm. Linear RGB colors are
 * decomposed into white, one secondary and one primary, see [linearToReflectance].
 */
private val BASE_M = doubleArrayOf(
    0.990673557319988, 0.990671524961979, 0.990662582353421, 0.990618107644795, 0.99045148087871,
    0.989871081400204, 0.98828660875964, 0.984290692797504, 0.973934905625306, 0.941817838460145,
    0.817390326195156, 0.432472805065729, 0.13845397825887, 0.0537347216940033, 0.0292174996673231,
    0.021313651750859, 0.0201349530181136, 0.0241323096280662, 0.0372236145223627, 0.0760506552706601,
    0.205375471942399, 0.541268903460439, 0.815841685086486, 0.912817704123976, 0.946339830166962,
    0.959927696331991, 0.966260595230312, 0.969325970058424, 0.970854536721399, 0.971605066528128,
    0.971962769757392, 0.972127272274509, 0.972209417745812, 0.972249577678424, 0.972267621998742,
    0.97227650946215, 0.972280243306874, 0.97228132482656
)

/**
 * Reflectance basis spectrum for yellow, 38 samples from 380 to 750 nm in steps of 10 nm. Linear RGB colors are
 * decomposed into white, one secondary and one primary, see [linearToReflectance].
 */
private val BASE_Y = doubleArrayOf(
    0.0210523371789306, 0.0210564627517414, 0.0210746178695038, 0.0211649058448753, 0.0215027957272504,
    0.0226738799041561, 0.0258235649693629, 0.0334879385639851, 0.0519069663740307, 0.100749014833473,
    0.239129899706847, 0.534804312272748, 0.79780757864303, 0.911449894067384, 0.953797963004507,
    0.971241615465429, 0.979303123807588, 0.983380119507575, 0.985461246567755, 0.986435046976605,
    0.986738250670141, 0.986617882445032, 0.986277776758643, 0.985860592444056, 0.98547492767621,
    0.985176934765558, 0.984971574014181, 0.984846303415712, 0.984775351811199, 0.984738066625265,
    0.984719648311765, 0.984711023391939, 0.984706683300676, 0.984704554393091, 0.98470359630937,
    0.984703124077552, 0.98470292561509, 0.984702868122795
)

/**
 * Reflectance basis spectrum for red, 38 samples from 380 to 750 nm in steps of 10 nm. Linear RGB colors are
 * decomposed into white, one secondary and one primary, see [linearToReflectance].
 */
private val BASE_R = doubleArrayOf(
    0.0315605737777207, 0.0315520718330149, 0.0315148215513658, 0.0313318044982702, 0.0306729857725527,
    0.0286480476989607, 0.0246450407045709, 0.0192960753663651, 0.0142066612220556, 0.0102942608878609,
    0.0076191460521811, 0.005898041083542, 0.0048233247781713, 0.0042298748350633, 0.0040599171299341,
    0.0043533695594676, 0.0053434425970201, 0.0076917201010463, 0.0135969795736536, 0.0316975442661115,
    0.107861196355249, 0.463812603168704, 0.847055405272011, 0.943185409393918, 0.968862150696558,
    0.978030667473603, 0.982043643854306, 0.983923623718707, 0.984845484154382, 0.985294275814596,
    0.985507295219825, 0.985605071539837, 0.985653849933578, 0.985677685033883, 0.985688391806122,
    0.985693664690031, 0.985695879848205, 0.985696521463762
)

/**
 * Reflectance basis spectrum for green, 38 samples from 380 to 750 nm in steps of 10 nm. Linear RGB colors are
 * decomposed into white, one secondary and one primary, see [linearToReflectance].
 */
private val BASE_G = doubleArrayOf(
    0.0095560747554212, 0.0095581580120851, 0.0095673245444588, 0.0096129126297349, 0.0097837090401843,
    0.010378622705871, 0.0120026452378567, 0.0160977721473922, 0.026706190223168, 0.0595555440185881,
    0.186039826532826, 0.570579820116159, 0.861467768400292, 0.945879089767658, 0.970465486474305,
    0.97841363028445, 0.979589031411224, 0.975533536908632, 0.962288755397813, 0.92312157451312, 0.793434018943111,
    0.459270135902429, 0.185574103666303, 0.0881774959955372, 0.05436302287667, 0.0406288447060719,
    0.034221520431697, 0.0311185790956966, 0.0295708898336134, 0.0288108739348928, 0.0284486271324597,
    0.0282820301724731, 0.0281988376490237, 0.0281581655342037, 0.0281398910216386, 0.0281308901665811,
    0.0281271086805816, 0.0281260133612096
)

/**
 * Reflectance basis spectrum for blue, 38 samples from 380 to 750 nm in steps of 10 nm. Linear RGB colors are
 * decomposed into white, one secondary and one primary, see [linearToReflectance].
 */
private val BASE_B = doubleArrayOf(
    0.979404752502014, 0.97940070684313, 0.979382903470261, 0.979294364945594, 0.97896301460857, 0.977814466694043,
    0.974724321133836, 0.967198482343973, 0.949079657530575, 0.900850128940977, 0.76315044546224,
    0.465922171649319, 0.201263280451005, 0.0877524413419623, 0.0457176793291679, 0.0284706050521843,
    0.020527176756985, 0.0165302792310211, 0.0145135107212858, 0.0136003508637687, 0.0133604258769571,
    0.013548894314568, 0.0139594356366992, 0.014443425575357, 0.0148854440621406, 0.0152254296999746,
    0.0154592848180209, 0.0156018026485961, 0.0156824871281936, 0.0157248764360615, 0.0157458108784121,
    0.0157556123350225, 0.0157605443964911, 0.0157629637515278, 0.0157640525629106, 0.015764589232951,
    0.0157648147772649, 0.0157648801149616
)

/**
 * The CIE 1931 2° color matching function x̄ multiplied by the CIE D65 illuminant and normalized so that a
 * perfect white reflector has Y = 1, 38 samples from 380 to 750 nm in steps of 10 nm. See spectral.md.
 */
private val CIE_CMF_X = doubleArrayOf(
    6.46919989576e-05, 0.0002194098998132, 0.0011205743509343, 0.0037666134117111, 0.011880553603799,
    0.0232864424191771, 0.0345594181969747, 0.0372237901162006, 0.0324183761091486, 0.021233205609381,
    0.0104909907685421, 0.0032958375797931, 0.0005070351633801, 0.0009486742057141, 0.0062737180998318,
    0.0168646241897775, 0.028689649025981, 0.0426748124691731, 0.0562547481311377, 0.0694703972677158,
    0.0830531516998291, 0.0861260963002257, 0.0904661376847769, 0.0850038650591277, 0.0709066691074488,
    0.0506288916373645, 0.035473961885264, 0.0214682102597065, 0.0125164567619117, 0.0068045816390165,
    0.0034645657946526, 0.0014976097506959, 0.000769700480928, 0.0004073680581315, 0.0001690104031614,
    9.52245150365e-05, 4.90309872958e-05, 1.99961492222e-05
)

/**
 * The CIE 1931 2° color matching function ȳ multiplied by the CIE D65 illuminant and normalized so that a
 * perfect white reflector has Y = 1, 38 samples from 380 to 750 nm in steps of 10 nm. See spectral.md.
 */
private val CIE_CMF_Y = doubleArrayOf(
    1.844289444e-06, 6.2053235865e-06, 3.10096046799e-05, 0.0001047483849269, 0.0003536405299538,
    0.0009514714056444, 0.0022822631748318, 0.004207329043473, 0.0066887983719014, 0.0098883960193565,
    0.0152494514496311, 0.0214183109449723, 0.0334229301575068, 0.0513100134918512, 0.070402083939949,
    0.0878387072603517, 0.0942490536184085, 0.0979566702718931, 0.0941521856862608, 0.0867810237486753,
    0.0788565338632013, 0.0635267026203555, 0.05374141675682, 0.042646064357412, 0.0316173492792708,
    0.020885205921391, 0.0138601101360152, 0.0081026402038399, 0.004630102258803, 0.0024913800051319,
    0.0012593033677378, 0.000541646522168, 0.0002779528920067, 0.0001471080673854, 6.10327472927e-05,
    3.43873229523e-05, 1.77059860053e-05, 7.220974913e-06
)

/**
 * The CIE 1931 2° color matching function z̄ multiplied by the CIE D65 illuminant and normalized so that a
 * perfect white reflector has Y = 1, 38 samples from 380 to 750 nm in steps of 10 nm. See spectral.md.
 */
private val CIE_CMF_Z = doubleArrayOf(
    0.000305017147638, 0.0010368066663574, 0.0053131363323992, 0.0179543925899536, 0.0570775815345485,
    0.113651618936287, 0.17335872618355, 0.196206575558657, 0.186082370706296, 0.139950475383207,
    0.0891745294268649, 0.0478962113517075, 0.0281456253957952, 0.0161376622950514, 0.0077591019215214,
    0.0042961483736618, 0.0020055092122156, 0.0008614711098802, 0.0003690387177652, 0.0001914287288574,
    0.0001495555858975, 9.23109285104e-05, 6.81349182337e-05, 2.88263655696e-05, 1.57671820553e-05,
    3.9406041027e-06, 1.584012587e-06, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
)

/**
 * Decomposes a color into weights for the reflectance basis spectra (Smits 1999).
 *
 * @param rgb The color to decompose.
 * @return The weights for white, cyan, magenta, yellow, red, green, and blue.
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
            weights[0] * BASE_W[i] +
                    weights[1] * BASE_C[i] +
                    weights[2] * BASE_M[i] +
                    weights[3] * BASE_Y[i] +
                    weights[4] * BASE_R[i] +
                    weights[5] * BASE_G[i] +
                    weights[6] * BASE_B[i]
        )
    }
    return reflectance
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
    return mixKubelkaMunk(listOf(reflectance1, reflectance2), listOf(1.0 - t, t), listOf(1.0, 1.0), k1, k2)
}

/**
 * The smallest luminance used for weighting a color in a mix, so that black colors still count.
 */
private const val MIN_LUMINANCE = 2.220446049250313E-16

/**
 * Mixes reflectance spectra with single-constant Kubelka–Munk theory, as in spectral.js 3.0. The K/S values of the
 * spectra are averaged per wavelength with weights `factor² · tintingStrength² · luminance`, where the luminance is
 * the Y of the spectrum. With non-zero Saunderson coefficients the spectra are converted to internal reflectance
 * before mixing and the result back to reflectance seen from outside.
 */
private fun mixKubelkaMunk(
    reflectances: List<DoubleArray>,
    factors: List<Double>,
    tintingStrengths: List<Double>,
    k1: Double,
    k2: Double
): DoubleArray {
    require(reflectances.isNotEmpty()) { "nothing to mix" }
    val concentrations = reflectances.indices.map { j ->
        val luminance = max(MIN_LUMINANCE, reflectances[j].dot(CIE_CMF_Y))
        factors[j] * factors[j] * tintingStrengths[j] * tintingStrengths[j] * luminance
    }
    val total = concentrations.sum()
    require(total > 0.0) { "at least one factor and tinting strength must be non-zero" }

    return DoubleArray(38) { i ->
        var ks = 0.0
        for (j in reflectances.indices) {
            if (concentrations[j] == 0.0) continue
            // Kubelka–Munk works with the internal reflectance of the layer, without the reflections at its surface,
            // reflectances are kept above 0 so that K/S stays finite
            val r = inverseSaundersonCorrection(max(reflectances[j][i], 0.00000001), k1, k2)
            ks += (pow(1.0 - r, 2.0) / (2.0 * r)) * concentrations[j]
        }
        ks /= total
        saundersonCorrection(1.0 + ks - sqrt(pow(ks, 2.0) + 2.0 * ks), k1, k2)
    }
}

/**
 * Converts a reflectance spectrum to the color of the surface under D65 light, in linear RGB.
 *
 * @param reflectance A reflectance spectrum with 38 samples from 380 to 750 nm in steps of 10 nm.
 */
fun reflectanceToRGBa(reflectance: DoubleArray): ColorRGBa = reflectanceToXYZ(reflectance).toRGBa()

/**
 * How [SpectralColor.toRGBa] and the spectral mixing functions bring colors outside the sRGB gamut into it.
 */
enum class SpectralGamutMapping {
    /** Colors are returned as they are, components can be below 0 or above 1. */
    NONE,

    /** Every linear RGB component is clamped to [0, 1], which can change hue and lightness. */
    CLIP,

    /**
     * Chroma is reduced in OKLCh, keeping lightness and hue, until the color is in gamut or the remaining difference
     * after clamping is below a just noticeable difference. This is the method of spectral.js 3.0 and of CSS Color 4.
     */
    MAP
}

private fun ColorRGBa.isInLinearGamut() = r in 0.0..1.0 && g in 0.0..1.0 && b in 0.0..1.0

private fun ColorRGBa.clampLinear() =
    ColorRGBa(r.coerceIn(0.0, 1.0), g.coerceIn(0.0, 1.0), b.coerceIn(0.0, 1.0), alpha, Linearity.LINEAR)

private fun deltaEOK(a: ColorOKLABa, b: ColorOKLABa): Double {
    val dl = a.l - b.l
    val da = a.a - b.a
    val db = a.b - b.b
    return sqrt(dl * dl + da * da + db * db)
}

/**
 * Maps a color into the sRGB gamut by reducing its OKLCh chroma, as spectral.js 3.0 and CSS Color 4 do.
 *
 * @param color the color, in any linearity
 * @param jnd the just noticeable difference in OKLab, colors that clamping changes less than this are clamped
 * @param epsilon the precision of the binary search over chroma
 * @return the mapped color in linear RGB
 */
private fun gamutMapOKLCh(color: ColorRGBa, jnd: Double = 0.03, epsilon: Double = 0.0001): ColorRGBa {
    val linear = color.toLinear()
    val lch = ColorOKLABa.fromRGBa(linear).toOKLCHa()
    if (lch.l >= 1.0) return ColorRGBa(1.0, 1.0, 1.0, linear.alpha, Linearity.LINEAR)
    if (lch.l <= 0.0) return ColorRGBa(0.0, 0.0, 0.0, linear.alpha, Linearity.LINEAR)
    if (linear.isInLinearGamut()) return linear

    var clipped = ColorOKLABa.fromRGBa(linear.clampLinear())
    var e = deltaEOK(clipped, ColorOKLABa.fromRGBa(linear))
    if (e < jnd) return clipped.toRGBa().clampLinear()

    var min = 0.0
    var max = lch.c
    var minInGamut = true
    while (max - min > epsilon) {
        val chroma = (min + max) / 2.0
        val oklab = ColorOKLCHa(lch.l, chroma, lch.h, linear.alpha).toOKLABa()
        val current = oklab.toRGBa()
        if (minInGamut && current.isInLinearGamut()) {
            min = chroma
        } else {
            clipped = ColorOKLABa.fromRGBa(current.clampLinear())
            e = deltaEOK(clipped, oklab)
            if (e < jnd) {
                if (jnd - e < epsilon) break
                minInGamut = false
                min = chroma
            } else {
                max = chroma
            }
        }
    }
    return clipped.toRGBa().clampLinear()
}

private fun ColorRGBa.mapToGamut(mapping: SpectralGamutMapping): ColorRGBa = when (mapping) {
    SpectralGamutMapping.NONE -> this
    SpectralGamutMapping.CLIP -> toLinear().clampLinear()
    SpectralGamutMapping.MAP -> gamutMapOKLCh(this)
}

/**
 * A color as a reflectance spectrum with a tinting strength, the color type of spectral.js 3.0.
 *
 * The tinting strength scales how strongly the color affects a mix: its concentration in a mix is
 * `factor² · tintingStrength² · luminance`. Strong pigments such as phthalo blue have a high tinting strength, weak
 * ones such as most earth pigments a low one.
 *
 * @param spectrum the reflectance spectrum, 38 samples from 380 to 750 nm in steps of 10 nm, it is copied
 * @param tintingStrength the tinting strength, 1.0 by default
 */
class SpectralColor(spectrum: DoubleArray, val tintingStrength: Double = 1.0) {
    init {
        require(spectrum.size == 38) { "spectrum must have 38 samples" }
    }

    internal val samples: DoubleArray = spectrum.copyOf()

    /**
     * A spectral color for an RGB color, using the reflectance spectrum from [linearToReflectance].
     */
    constructor(color: ColorRGBa, tintingStrength: Double = 1.0) : this(linearToReflectance(color), tintingStrength)

    /** A copy of the reflectance spectrum */
    val reflectance: DoubleArray get() = samples.copyOf()

    /** The luminance Y of the color under D65 light */
    val luminance: Double by lazy { reflectanceToXYZ(samples).y }

    /** Returns this color with a different tinting strength */
    fun withTintingStrength(tintingStrength: Double) = SpectralColor(samples, tintingStrength)

    /** The color in CIE XYZ under D65 light */
    fun toXYZa(): ColorXYZa = reflectanceToXYZ(samples)

    /**
     * The color in linear RGB under D65 light.
     * @param gamutMapping how colors outside the sRGB gamut are mapped into it
     */
    fun toRGBa(gamutMapping: SpectralGamutMapping = SpectralGamutMapping.NONE): ColorRGBa =
        reflectanceToRGBa(samples).mapToGamut(gamutMapping)
}

/**
 * Mixes any number of spectral colors with Kubelka–Munk theory, as `spectral.mix` in spectral.js 3.0.
 *
 * Every color is given with its mixing factor. Its concentration in the mix is
 * `factor² · tintingStrength² · luminance`, the factors do not need to add up to 1.
 *
 * @param colors the colors with their mixing factors, at least one factor must be non-zero
 * @param k1 The Saunderson coefficient for reflection at the front surface, see [mixSpectral].
 * @param k2 The Saunderson coefficient for internal reflection at the surface, see [mixSpectral].
 * @return the mix, with a tinting strength of 1
 */
fun mixSpectral(vararg colors: Pair<SpectralColor, Double>, k1: Double = 0.0, k2: Double = 0.0): SpectralColor {
    require(colors.isNotEmpty()) { "nothing to mix" }
    return SpectralColor(
        mixKubelkaMunk(
            colors.map { it.first.samples }, colors.map { it.second }, colors.map { it.first.tintingStrength }, k1, k2
        )
    )
}

/**
 * Mixes any number of RGB colors with Kubelka–Munk theory, see the [SpectralColor] version of [mixSpectral].
 * Use [SpectralColor] to give colors a tinting strength.
 *
 * @param colors the colors with their mixing factors, at least one factor must be non-zero
 * @param gamutMapping how a mix outside the sRGB gamut is mapped into it
 * @return the mix, with the linearity of the first color
 */
@JvmName("mixSpectralRGBa")
fun mixSpectral(
    vararg colors: Pair<ColorRGBa, Double>,
    k1: Double = 0.0,
    k2: Double = 0.0,
    gamutMapping: SpectralGamutMapping = SpectralGamutMapping.MAP
): ColorRGBa {
    require(colors.isNotEmpty()) { "nothing to mix" }
    val mix = mixSpectral(*colors.map { SpectralColor(it.first) to it.second }.toTypedArray(), k1 = k1, k2 = k2)
    return mix.toRGBa(gamutMapping).matchLinearity(colors.first().first)
}

/**
 * Creates a palette of [size] colors from [a] to [b] by mixing them in spectral steps, as `spectral.palette` in
 * spectral.js 3.0. The first color is [a] and the last [b].
 */
fun spectralPalette(a: SpectralColor, b: SpectralColor, size: Int, k1: Double = 0.0, k2: Double = 0.0): List<SpectralColor> {
    require(size >= 1) { "size must be at least 1" }
    if (size == 1) return listOf(a)
    return List(size) { i -> mixSpectral(a to (size - 1 - i).toDouble(), b to i.toDouble(), k1 = k1, k2 = k2) }
}

/**
 * Creates a palette of [size] colors from [a] to [b] by mixing them in spectral steps, see the [SpectralColor]
 * version of [spectralPalette].
 *
 * @param gamutMapping how colors outside the sRGB gamut are mapped into it
 * @return the colors, with the linearity of [a]
 */
fun spectralPalette(
    a: ColorRGBa,
    b: ColorRGBa,
    size: Int,
    k1: Double = 0.0,
    k2: Double = 0.0,
    gamutMapping: SpectralGamutMapping = SpectralGamutMapping.MAP
): List<ColorRGBa> = spectralPalette(SpectralColor(a), SpectralColor(b), size, k1, k2).map {
    it.toRGBa(gamutMapping).matchLinearity(a)
}

/**
 * Samples a spectral gradient at [t], as `spectral.gradient` in spectral.js 3.0. The gradient is defined by [stops],
 * colors with their positions; between two stops the colors are mixed spectrally. Before the first and after the
 * last stop the gradient has the color of that stop.
 */
fun spectralGradient(t: Double, vararg stops: Pair<SpectralColor, Double>, k1: Double = 0.0, k2: Double = 0.0): SpectralColor {
    require(stops.isNotEmpty()) { "a gradient needs at least one stop" }
    var below: Pair<SpectralColor, Double>? = null
    var above: Pair<SpectralColor, Double>? = null
    for (stop in stops) {
        if (stop.second <= t && (below == null || stop.second > below.second)) below = stop
        if (stop.second >= t && (above == null || stop.second < above.second)) above = stop
    }
    if (below == null) return above!!.first
    if (above == null) return below.first
    if (below.second == above.second) return below.first
    val factor = (t - below.second) / (above.second - below.second)
    return mixSpectral(below.first to 1.0 - factor, above.first to factor, k1 = k1, k2 = k2)
}

/**
 * Samples a spectral gradient of RGB colors at [t], see the [SpectralColor] version of [spectralGradient].
 *
 * @param gamutMapping how colors outside the sRGB gamut are mapped into it
 * @return the color, with the linearity of the first stop
 */
@JvmName("spectralGradientRGBa")
fun spectralGradient(
    t: Double,
    vararg stops: Pair<ColorRGBa, Double>,
    k1: Double = 0.0,
    k2: Double = 0.0,
    gamutMapping: SpectralGamutMapping = SpectralGamutMapping.MAP
): ColorRGBa {
    require(stops.isNotEmpty()) { "a gradient needs at least one stop" }
    val spectral = stops.map { SpectralColor(it.first) to it.second }.toTypedArray()
    return spectralGradient(t, *spectral, k1 = k1, k2 = k2).toRGBa(gamutMapping).matchLinearity(stops.first().first)
}

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

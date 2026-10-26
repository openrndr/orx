# Spectral color mixing

`Spectral.kt` (package `org.openrndr.extra.color.mixing`) mixes colors the way paints mix instead of the way light
mixes: blue and yellow make green, not gray. It is a port of [spectral.js](https://github.com/rvanwijnen/spectral.js)
by Ronald van Wijnen, version 3.0.0, with an added Saunderson correction.

```kotlin
val green = mixSpectral(ColorRGBa.BLUE, ColorRGBa.YELLOW, 0.5)

// any number of colors with mixing factors, the result is mapped into the sRGB gamut
val brown = mixSpectral(ColorRGBa.RED to 1.0, ColorRGBa.YELLOW to 1.0, ColorRGBa.BLUE to 0.5)

// tinting strength makes a color weaker or stronger in a mix
val red = SpectralColor(ColorRGBa.RED, tintingStrength = 0.35)
val orange = mixSpectral(red to 0.5, SpectralColor(ColorRGBa.YELLOW) to 0.5).toRGBa(SpectralGamutMapping.MAP)

val palette = spectralPalette(ColorRGBa.fromHex("#BB0657"), ColorRGBa.fromHex("#E0E0B3"), 8)
val color = spectralGradient(
    0.75,
    ColorRGBa.fromHex("#005E72") to 0.0, ColorRGBa.fromHex("#EAD9A7") to 0.5, ColorRGBa.fromHex("#894B54") to 1.0
)

val r = linearToReflectance(color)     // 38 reflectance samples, 380..750 nm
val xyz = reflectanceToXYZ(r)          // CIE XYZ of that surface under D65

// measured spectra can be mixed directly
val mix = mixReflectance(ColorCheckerReflectances.blue, ColorCheckerReflectances.yellow, 0.5)
val rgb = reflectanceToRGBa(mix)       // linear RGB under D65
```

## API

| Function | Does |
|---|---|
| `mixSpectral(color1, color2, t, k1, k2)` | mixes two RGB colors, the result is not gamut mapped |
| `mixSpectral(vararg colors: Pair<ColorRGBa, Double>, k1, k2, gamutMapping)` | mixes any number of RGB colors with factors |
| `mixSpectral(vararg colors: Pair<SpectralColor, Double>, k1, k2)` | mixes spectral colors, returns a `SpectralColor` |
| `spectralPalette(a, b, size, ...)` | `size` colors from `a` to `b`, for RGB and spectral colors |
| `spectralGradient(t, vararg stops, ...)` | the color at `t` of a gradient with color stops, for RGB and spectral colors |
| `mixReflectance(reflectance1, reflectance2, t, k1, k2)` | mixes two reflectance spectra |
| `SpectralColor(color or spectrum, tintingStrength)` | a reflectance spectrum with a tinting strength |
| `linearToReflectance(color)`, `reflectanceToXYZ(r)`, `reflectanceToRGBa(r)` | conversions |

The RGB versions return colors with the linearity of their first color. Except for the two-color `mixSpectral`,
which keeps its old behavior, they map results into the sRGB gamut by default (`SpectralGamutMapping.MAP`), as
spectral.js does. The results match the examples in the spectral.js 3.0.0 README to within 1/255 (see
`TestSpectralJs`).

## Pipeline

Mixing works in five steps:

1. The colors are converted to linear sRGB.
2. Each color is turned into a reflectance spectrum: the fraction of light the surface reflects at each wavelength
   ([From RGB to a reflectance spectrum](#from-rgb-to-a-reflectance-spectrum)).
3. Each spectrum is converted to the Kubelka–Munk ratio of absorption to scattering, K/S. The ratios are averaged,
   weighted by concentration, and converted back to a reflectance spectrum ([Kubelka–Munk mixing](#kubelkamunk-mixing)).
   With non-zero Saunderson coefficients, the reflections at the paint surface are removed before this step and
   added back after it ([Saunderson correction](#saunderson-correction)).
4. The mixed spectrum is converted to the reflectance seen from outside with the Saunderson correction.
5. The spectrum is integrated against the color matching functions to give CIE XYZ, which is converted back to RGB
   ([The CIE_CMF_X, Y and Z tables](#the-cie_cmf_x-y-and-z-tables)) and, if requested, mapped into the sRGB gamut
   ([Gamut mapping](#gamut-mapping)).

All spectra use 38 samples from 380 nm to 750 nm in steps of 10 nm.

## The CIE_CMF_X, Y and Z tables

### Color matching functions

Human color vision is trichromatic: three types of cone cells with overlapping sensitivities reduce any light
spectrum to three numbers. Two spectra that produce the same three numbers look identical; such spectra are called
metamers. A set of color matching functions describes this reduction. They give, per wavelength, how much each of
three reference stimuli a spectrum contributes.

The functions used here are those of the **CIE 1931 2° standard observer**. They were derived from color matching
experiments by W. D. Wright (1928–1929, ten observers) and J. Guild (1931, seven observers). Observers adjusted the
intensities of three monochromatic primaries until their mix matched a monochromatic test light, within a 2° field
of view that falls on the fovea. This gave the r̄, ḡ, b̄ functions.

Some wavelengths can only be matched by adding a primary to the *test* side, so these functions go negative. In 1931
the CIE transformed them linearly into x̄, ȳ, z̄, which are positive everywhere, and chose the transform so that:

- ȳ equals the photopic luminous efficiency function V(λ), so Y is luminance;
- an equal-energy spectrum has X = Y = Z;
- z̄ is zero at long wavelengths.

x̄ peaks near 600 nm (with a secondary lobe near 450 nm), ȳ near 555 nm and z̄ near 450 nm. The same peaks show up
in the tables: X at 600, Y at 550 and Z at 450 nm on the 10 nm grid.

### Tristimulus values of a surface

A surface does not emit light. Its color depends on the light that falls on it, the illuminant S(λ). The
tristimulus values of a surface with reflectance R(λ) are

```
X = k Σ R(λ) S(λ) x̄(λ) Δλ
Y = k Σ R(λ) S(λ) ȳ(λ) Δλ
Z = k Σ R(λ) S(λ) z̄(λ) Δλ        with  k = 1 / Σ S(λ) ȳ(λ) Δλ
```

The normalization k makes a perfect white reflector (R = 1 everywhere) have Y = 1.

### What is in the tables

`CIE_CMF_X`, `CIE_CMF_Y` and `CIE_CMF_Z` are not the raw x̄, ȳ, z̄ (ȳ peaks at 1.0). Each entry is the product
`k · S(λ) · x̄(λ) · Δλ` with S = **CIE standard illuminant D65**, the white point of sRGB, sampled at the 38
wavelengths. With the illuminant and normalization baked in, `reflectanceToXYZ` reduces to three dot products.

This can be checked against the published tables. At 450, 500, 550, 600 and 650 nm the ratio
`CIE_CMF_Y / (ȳ · D65)` is the same constant, 0.000946 = k · Δλ. The sums over all 38 entries, which are the XYZ of
a perfect white reflector, are:

| | sum of the table | D65 white point |
|---|---|---|
| X | 0.95016 | 0.95047 |
| Y | 1.00000 | 1.00000 |
| Z | 1.08813 | 1.08883 |

The small differences come from sampling at 10 nm and ending at 750 nm instead of 780 nm. Because the white of the
tables is D65, the XYZ result can go straight through the sRGB matrices (which also assume D65) without chromatic
adaptation.

## From RGB to a reflectance spectrum

Infinitely many reflectance spectra map to the same RGB color (metamerism), so RGB-to-spectrum conversion has to
pick one. `linearToReflectance` uses the method of Brian Smits, *An RGB-to-Spectrum Conversion for Reflectances*
(1999). The linear RGB color is split into:

- **white**: min(r, g, b);
- one **secondary** (cyan, magenta or yellow) for what the two largest channels share beyond white;
- one **primary** (red, green or blue) for what is left in the largest channel.

The reflectance is the weighted sum of seven fixed basis spectra, `BASE_W`, `BASE_C`, `BASE_M`, `BASE_Y`, `BASE_R`,
`BASE_G` and `BASE_B`: reflectance curves between 0 and 1 computed for spectral.js such that under D65 each maps back
to its sRGB color. White is nearly flat, between 0.9997 and 1.0012. The data is that of spectral.js 3.0.0. The
spectral.js 2 basis spectra this port started from had a discontinuity in magenta and green: magenta dropped to almost
0 at 570 and 580 nm and jumped to 0.85 at 590 nm, green did the opposite.

Reflectances are clamped to at least 1·10⁻⁸ so that K/S below stays finite.

## Kubelka–Munk mixing

Paint is a layer of pigment particles in a binder. Light entering it is absorbed and scattered many times before
some of it leaves again. P. Kubelka and F. Munk (1931) modeled this with two diffuse fluxes, one going down and one
going up, governed by an absorption coefficient K and a scattering coefficient S. For a layer thick enough to hide
what is beneath it, the reflectance R∞ depends only on their ratio:

```
K/S = (1 − R∞)² / (2 R∞)
R∞  = 1 + K/S − √((K/S)² + 2 K/S)
```

For a mixture, absorption and scattering add up, weighted by concentration (Duncan, 1940): K = Σ cᵢKᵢ and
S = Σ cᵢSᵢ. spectral.js uses the **single-constant** simplification, which assumes all pigments scatter equally, so
that K/S itself mixes linearly. Per wavelength:

```
(K/S)mix = Σ cᵢ · (K/S)ᵢ / Σ cᵢ
```

Absorption mixes nonlinearly in reflectance: a little dark pigment absorbs a lot, which is why blue and yellow give
green and not gray. In the code, `ks` is the mixed K/S and `km` is the resulting R∞.

### Concentration

The mixing factors are not used as concentrations directly. As in spectral.js 3.0, the concentration of a color is

```
c = f² · T² · L
```

with mixing factor f, tinting strength T and luminance L, the Y of its reflectance spectrum. Dark colors absorb
strongly and would otherwise dominate the mix; weighting by luminance makes the factors behave more evenly. The
tinting strength lets a color be weaker or stronger in mixes, like a strong or weak pigment. Both are design choices
of spectral.js, not part of Kubelka–Munk theory. The factors do not need to add up to 1. For two colors with factors
1 − t and t this gives the `t` of the two-color `mixSpectral`.

### Limitations

- Layers are assumed opaque. Thin or transparent layers (glazes) need the finite-thickness Kubelka–Munk equations
  and a background color.
- Illumination is assumed diffuse.
- All pigments are assumed to scatter equally (the single-constant assumption).
- The spectra come from RGB colors, not from measured pigments. The result is a plausible paint-like mix, not a
  prediction for real paints.
- The mixed spectrum is a valid reflectance between 0 and 1, but its color can still lie slightly outside the sRGB
  gamut. Green mixed with blue gives a negative red component, cyan with magenta a blue above 1.

## Gamut mapping

`SpectralGamutMapping` chooses what happens to colors outside the sRGB gamut:

- `NONE` returns them unchanged;
- `CLIP` clamps every linear RGB component to [0, 1], which can shift hue and lightness;
- `MAP` reduces the chroma in OKLCh while keeping lightness and hue, until the color is in gamut or clamping it
  changes it less than a just noticeable difference of 0.03 in OKLab. This is the method of spectral.js 3.0 and the
  gamut mapping algorithm of CSS Color 4.

## Saunderson correction

Kubelka–Munk describes light *inside* the paint layer. A real paint surface is also a boundary between air and a
binder with a refractive index around 1.5, and that boundary reflects light both ways. J. L. Saunderson (1942)
added these surface reflections. If Rᵢ is the internal reflectance from Kubelka–Munk, the reflectance seen from
outside is

```
Rₘ = k₁ + (1 − k₁)(1 − k₂) Rᵢ / (1 − k₂ Rᵢ)
```

- **k₁** is the fraction of incoming light reflected at the front surface before entering the paint: the gloss or
  specular reflection. For light at normal incidence it follows from Fresnel's equation, ((n − 1)/(n + 1))², which
  is about 0.04 for n = 1.5. Measurements that exclude the specular component use k₁ = 0.
- **k₂** is the fraction of diffuse light inside the paint that is reflected back in at the surface instead of
  leaving it. Total internal reflection makes this large: about 0.4 to 0.6 for n = 1.5.

The terms mean: k₁ is the light reflected straight away; (1 − k₁) of the light enters; (1 − k₂) of what comes back
up gets out; and 1/(1 − k₂Rᵢ) is the sum of the repeated bounces between the surface and the paint.

`saundersonCorrection(rInf, k1, k2)` implements this formula: `rInf` is the internal Kubelka–Munk reflectance and the
result is the reflectance seen from outside. With k₁ = k₂ = 0 it returns its input, so it has no effect at the
default parameters.

### Applying it in both directions

The spectra from `linearToReflectance` describe the colors as seen from outside. `mixSpectral` therefore first
converts them to internal reflectance with the inverse correction, `inverseSaundersonCorrection`:

```
Rᵢ = (Rₘ − k₁) / (1 − k₁ − k₂ + k₂ Rₘ)
```

Then it mixes them with Kubelka–Munk and converts the result back with the forward formula. Mixing a color with
itself returns that color for any k₁ and k₂, and t = 0 and t = 1 return the input colors.

A glossy surface never looks darker than its own surface reflection k₁. A reflectance below k₁ has no internal
equivalent: the inverse clamps it, and such a color comes back lifted to at least k₁ at those wavelengths. With
k₁ = 0.04 this affects very dark and very saturated colors, the ones that reflect less than 4% at some wavelengths.
Earlier versions applied only the forward correction, which added the surface reflection a second time and changed
colors even when they were mixed with themselves.

## Reflectance presets

`ColorCheckerReflectances` contains the measured reflectance spectra of the 24 patches of the X-Rite (formerly
Macbeth) ColorChecker Classic: skin tones, sky, foliage, primaries, secondaries and six neutrals. The data was
measured by N. Ohta (1997) and is identical to the data in ISO 17321-1. It was taken from
[colour-science](https://github.com/colour-science/colour) (BSD-3-Clause) and resampled from 5 nm to the 10 nm grid
by taking every other sample.

These are real spectra instead of spectra upsampled from RGB, so mixing them with `mixReflectance` shows how
surfaces with these specific spectral shapes combine. Rendered with `reflectanceToRGBa`, they match the published
sRGB values of the chart. The cyan patch lies slightly outside the sRGB gamut, as it does on the physical chart.

`mixReflectance(reflectance1, reflectance2, t, k1, k2)` is the spectral core of `mixSpectral`: the same
concentration weighting, Kubelka–Munk mixing and Saunderson correction, without the conversion from and to RGB.

## Differences from spectral.js 3.0

- The Saunderson correction is an addition of this port; spectral.js does not model surface reflections.
- XYZ is converted to RGB with the openrndr matrices, results can differ from spectral.js by 1/255.
- Out-of-gamut RGB input is handled by clamping the decomposition weights to 0.
- Alpha is ignored: mixes are always opaque.
- spectral.js parses CSS color strings and formats results as hex; use `ColorRGBa.fromHex` and openrndr's formatting
  instead.

## References

- CIE 15:2018, *Colorimetry*, 4th edition. Color matching functions, standard illuminant D65, and tristimulus
  computation.
- W. D. Wright, *A re-determination of the trichromatic coefficients of the spectral colours*, Transactions of the
  Optical Society 30 (1928–29).
- J. Guild, *The colorimetric properties of the spectrum*, Philosophical Transactions of the Royal Society A 230 (1931).
- B. Smits, *An RGB-to-Spectrum Conversion for Reflectances*, Journal of Graphics Tools 4(4) (1999).
- P. Kubelka and F. Munk, *Ein Beitrag zur Optik der Farbanstriche*, Zeitschrift für technische Physik 12 (1931).
- D. R. Duncan, *The colour of pigment mixtures*, Proceedings of the Physical Society 52 (1940).
- J. L. Saunderson, *Calculation of the color of pigmented plastics*, Journal of the Optical Society of America
  32(12) (1942).
- R. van Wijnen, spectral.js, https://github.com/rvanwijnen/spectral.js

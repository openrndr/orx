# Spectral color mixing

`Spectral.kt` (package `org.openrndr.extra.color.mixing`) mixes colors the way paints mix instead of the way light
mixes: blue and yellow make green, not gray. It is a port of [spectral.js](https://github.com/rvanwijnen/spectral.js)
by Ronald van Wijnen, from before its 3.0 release.

```kotlin
val green = mixSpectral(ColorRGBa.BLUE, ColorRGBa.YELLOW, 0.5)

val r = linearToReflectance(color)     // 38 reflectance samples, 380..750 nm
val xyz = reflectanceToXYZ(r)          // CIE XYZ of that surface under D65

// measured spectra can be mixed directly
val mix = mixReflectance(ColorCheckerReflectances.blue, ColorCheckerReflectances.yellow, 0.5)
val rgb = reflectanceToRGBa(mix)       // linear RGB under D65
```

## Pipeline

`mixSpectral(color1, color2, t, k1, k2)` works in five steps:

1. Both colors are converted to linear sRGB.
2. Each color is turned into a reflectance spectrum: the fraction of light the surface reflects at each wavelength
   ([From RGB to a reflectance spectrum](#from-rgb-to-a-reflectance-spectrum)).
3. Each spectrum is converted to the Kubelka–Munk ratio of absorption to scattering, K/S. The ratios are averaged,
   weighted by concentration, and converted back to a reflectance spectrum ([Kubelka–Munk mixing](#kubelkamunk-mixing)).
   With non-zero Saunderson coefficients, the reflections at the paint surface are removed before this step and
   added back after it ([Saunderson correction](#saunderson-correction)).
4. The mixed spectrum is converted to the reflectance seen from outside with the Saunderson correction.
5. The spectrum is integrated against the color matching functions to give CIE XYZ, which is converted back to RGB
   with the linearity of `color1` ([The CIE_CMF_X, Y and Z tables](#the-cie_cmf_x-y-and-z-tables)).

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

The reflectance is the weighted sum of seven fixed basis spectra. White is a flat spectrum at 1.0; the other six are
`SPD_C`, `SPD_M`, `SPD_Y`, `SPD_R`, `SPD_G` and `SPD_B`. Despite the `SPD` (spectral power distribution) prefix,
these are reflectance curves between 0 and 1, not emission spectra. They are smooth curves computed for spectral.js
such that under D65 each maps back to its sRGB color. Converting linear RGB to a spectrum and back through
`reflectanceToXYZ` reproduces the input to within 0.001.

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
(K/S)mix = (1 − c) · (K/S)₁ + c · (K/S)₂
```

Absorption mixes nonlinearly in reflectance: a little dark pigment absorbs a lot, which is why blue and yellow give
green and not gray. In the code, `ks` is the mixed K/S and `km` is the resulting R∞.

### Concentration

`t` is not used as the concentration directly. `linearToConcentration` weights each color by its luminance l (the Y
of its reflectance spectrum):

```
c = l₂ t² / (l₁ (1 − t)² + l₂ t²)
```

Dark colors absorb strongly and would otherwise dominate the mix. This weighting makes `t` behave more evenly. It is
a design choice of spectral.js, not part of Kubelka–Munk theory. spectral.js 3.0 generalizes it to
`C = f² · T² · L`, with mixing factor f, tinting strength T and luminance L.

### Limitations

- Layers are assumed opaque. Thin or transparent layers (glazes) need the finite-thickness Kubelka–Munk equations
  and a background color.
- Illumination is assumed diffuse.
- All pigments are assumed to scatter equally (the single-constant assumption).
- The spectra come from RGB colors, not from measured pigments. The result is a plausible paint-like mix, not a
  prediction for real paints.
- The mixed spectrum is a valid reflectance between 0 and 1, but its color can still lie slightly outside the sRGB
  gamut. Green mixed with blue gives about (−0.05, 0.25, 0.07) in linear RGB, and cyan with magenta a blue of 1.03.
  `mixSpectral` returns such values unclamped.

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

spectral.js 3.0 adds features this port does not have:

- mixing any number of colors with individual factors: `mix([c1, f1], [c2, f2], ...)`;
- a tinting strength per color, which scales its concentration;
- `palette(a, b, size)` and `gradient(t, ...colors)`;
- gamut mapping of the mixed result in OKLCh, by reducing chroma until it fits in sRGB;
- an explicit white basis spectrum instead of a flat 1.0, and basis spectra with more precise values.

spectral.js does not use a Saunderson correction; it was added in this port. `mixSpectral` also ignores alpha: the
result is always opaque.

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

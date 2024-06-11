# orx-fx

Ready-to-use GPU-based visual effects or filters. Most include
[orx-parameters](https://github.com/openrndr/orx/tree/master/orx-parameters) annotations 
so they can be easily controlled via orx-gui.

The provided filters are based on OPENRNDR's 
[`Filter` class](https://api.openrndr.org/openrndr-draw/org.openrndr.draw/-filter/index.html)

All filters provided by orx-fx assume pre-multiplied alpha inputs, which is OPENRNDR's default.

## Effects index

Here's a (potentially incomplete) list of the effects provided by orx-fx. Explore [the source](https://github.com/openrndr/orx/tree/master/orx-fx/src/commonMain/kotlin) for an up-to-date list.

### Anti-alias

 - `FXAA`, fast approximate anti-aliasing.
 
### Blends

Blend filters take two inputs ("source" and "destination"), they are intended to be used in `orx-compositor`'s layer blend. All blend filters are opacity preserving.

#### Photoshop-style blends

 - `ColorBurn`
 - `ColorDodge`
 - `Darken`
 - `HardLight`
 - `Lighten`
 - `Multiply`
 - `Normal`
 - `Overlay`
 - `Screen`
 - `Add`, add source and destination inputs
 - `Subtract`, substract destination color from source color

#### Porter-Duff blends

 - `SourceIn`, Porter-Duff source-in blend, intersect source and destination opacity and keep source colors
 - `SourceOut`, Porter-Duff source-out blend, subtract destination from source opacity and keep source colors
 - `SourceAtop`, Porter-Duff source-atop blend, uses destination opacity, layers source on top and keeps both colors
 - `DestinationIn`, Porter-Duff destination-in blend, intersect source and destination opacity and keep source colors
 - `DestinationOut`, Porter-Duff destination-out blend, subtract destination from source opacity and keep destination colors
 - `DestinationAtop`, Porter-Duff destination-atop blend, uses source opacity, layers destination on top and keeps both colors
 - `Xor`, Porter-Duff xor blend, picks colors from input with highest opacity or none with opacities are equal

#### Various blends

 - `Passthrough`, pass source color and opacity.
 
### Blurs

Most blur effects are opacity preserving

 - `ApproximateGaussianBlur`, a somewhat faster but less precise implementation of `GaussianBlur`
 - `Bloom`, a multi-pass bloom/glow effect
 - `BoxBlur`, a simple but fast box blur
 - `FrameBlur`
 - `GaussianBlur`, a slow but precise Gaussian blur
 - `HashBlur`, a noisy blur effect
 - `LaserBlur`
 - `LineBlur`
 - `MipBloom`
 - `ZoomBlur`, a directional blur with a zooming effect


### Color

 - `ChromaticAberration`, a chromatic aberration effect based on RGB color separation
 - `ColorCorrection`, corrections for brightness, contrast, saturation and hue
 - `ColorLookup`, Color LUT filter
 - `ColorMix`, filter implementation of OPENRNDR's color matrix mixing
 - `Duotone`, maps luminosity to two colors, very similar to `LumaMap` but uses LAB color interpolation. 
 - `DuotoneGradient`, a two-point gradient version of `Duotone`
 - `Invert`
 - `LumaMap`, maps luminosity to two colors
 - `LumaOpacity`, maps luminosity to opacity but retains source color
 - `LumaThreshold`, applies a treshold on the input luminosity and maps to two colors
 - `Posterize`, a posterize effect
 - `Sepia`, applies a reddish-brown monochrome tint that imitates an old photograph
 - `SetBackground`
 - `SubtractConstant`, subtract a constant color from the source color

### Color conversion

 - `OkLabToRgb`
 - `RgbToOkLab`
 
### Distortion

All distortion effects are opacity preserving

 - `BlockRepeat` - repeats a single configurable block of the source input
 - `DisplaceBlend`
 - `Fisheye`
 - `FluidDistort`
 - `Lenses`
 - `HorizontalWave` - applies a horizontal wave effect on the source input
 - `VerticalWave` - applies a vertical wave effect on the source input
 - `PerspectivePlane` - applies a planar perspective distortion on the source input
 - `Perturb`
 - `PolarToRectangular`
 - `RectangularToPolar`
 - `StackRepeat` - repeats the source input in a stack fashion
 - `StretchWaves`
 - `TapeNoise`
 - `Tiles`
 - `VideoGlitch`
 
### Dither

 - `ADither` - a selection of dithering effects
 - `CMYKHalftone` - a configurable CMYK halftoning effect
 - `Crosshatch` - crosshatching effect
 - `LumaHalftone` - a halftoning effect based on luminosity
 
### Edges
 
  - `LumaSobel` - A Sobel-kernel based luminosity edge detector
  - `EdgesWork` - An edges filter doubling as erosion
  - `Contour` - detects multi-level contours
  - New: `CannyEdgeDetector`
  
### Grain
 
  - `FilmGrain` - adds film-like grain to the source input
 
### Shadow
 
  - `DropShadow` - adds a drop shadow based on the opacity in the input image
  
### Tonemap
 
  - `Uncharted2Tonemap` - implements the Uncharted2 tonemapper
 
### Transform
 
  - `FlipVertically` - flips the source input vertically.
 
## `Post` extension

The `Post` extension provides an easy way to apply filters to your drawings. Allocating
and resizing color buffers is all taken care of by `Post`.

To get additional intermediate color buffers one can access `intermediate[x]`
```kotlin
fun main() = application {
    configure {
        windowResizable = true
    }
    program {
        extend(Post()) {
            val blur = ApproximateGaussianBlur()
            val add = Add()
            post { input, output ->
                blur.window = 50
                blur.sigma = 50.0
                blur.apply(input, intermediate[0])
                add.apply(arrayOf(input, intermediate[0]), output)
            }
        }
        extend {
            drawer.circle(width / 2.0, height / 2.0, 100.0)
        }
    }
}
```

### Colormap

Colormap filters operate only on the RED color channel. For example
depth maps from
[orx-depth-camera](https://github.com/openrndr/orx/tree/master/orx-depth-camera).

They allow selection of `min` / `max` value range and applying exponential
shaping `curve` within this range:

- `GrayscaleColormap` - maps to gray tones
- `SpectralZucconiColormap` - maps to natural light dispersion spectrum as described
  by Alan Zucconi in the
  [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/)
  article.
- `TurboColormap` - maps to Turbo Colormap according to
  [Turbo, An Improved Rainbow Colormap for Visualization](https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html)
  by Google.



<!-- __demos__ >
# Demos
[DemoFluidDistort01Kt](src/demo/kotlin/DemoFluidDistort01Kt.kt
![DemoFluidDistort01Kt](https://github.com/openrndr/orx/blob/media/orx-fx/images/DemoFluidDistort01Kt.png
<!-- __demos__ -->
## Demos
### DemoBlur01
[source code](src/jvmDemo/kotlin/DemoBlur01.kt)

![DemoBlur01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoBlur01Kt.png)

### DemoCannyEdgeDetector01
[source code](src/jvmDemo/kotlin/DemoCannyEdgeDetector01.kt)

![DemoCannyEdgeDetector01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoCannyEdgeDetector01Kt.png)

### DemoColorDuotone01
[source code](src/jvmDemo/kotlin/DemoColorDuotone01.kt)

![DemoColorDuotone01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoColorDuotone01Kt.png)

### DemoColorDuotoneGradient01
[source code](src/jvmDemo/kotlin/DemoColorDuotoneGradient01.kt)

![DemoColorDuotoneGradient01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoColorDuotoneGradient01Kt.png)

### DemoColormapGrayscale
[source code](src/jvmDemo/kotlin/DemoColormapGrayscale.kt)

![DemoColormapGrayscaleKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoColormapGrayscaleKt.png)

### DemoColormapSpectralZucconi
[source code](src/jvmDemo/kotlin/DemoColormapSpectralZucconi.kt)

![DemoColormapSpectralZucconiKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoColormapSpectralZucconiKt.png)

### DemoColormapTurbo
[source code](src/jvmDemo/kotlin/DemoColormapTurbo.kt)

![DemoColormapTurboKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoColormapTurboKt.png)

### DemoColorPosterize01
[source code](src/jvmDemo/kotlin/DemoColorPosterize01.kt)

![DemoColorPosterize01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoColorPosterize01Kt.png)

### DemoCompositeFilter01
[source code](src/jvmDemo/kotlin/DemoCompositeFilter01.kt)

![DemoCompositeFilter01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoCompositeFilter01Kt.png)

### DemoDirectionalBlur01
[source code](src/jvmDemo/kotlin/DemoDirectionalBlur01.kt)

![DemoDirectionalBlur01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoDirectionalBlur01Kt.png)

### DemoDistortLenses01
[source code](src/jvmDemo/kotlin/DemoDistortLenses01.kt)

![DemoDistortLenses01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoDistortLenses01Kt.png)

### DemoDitherLumaHalftone01
[source code](src/jvmDemo/kotlin/DemoDitherLumaHalftone01.kt)

![DemoDitherLumaHalftone01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoDitherLumaHalftone01Kt.png)

### DemoFluidDistort01
[source code](src/jvmDemo/kotlin/DemoFluidDistort01.kt)

![DemoFluidDistort01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoFluidDistort01Kt.png)

### DemoOkLab01
[source code](src/jvmDemo/kotlin/DemoOkLab01.kt)

![DemoOkLab01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoOkLab01Kt.png)

### DemoPost01
[source code](src/jvmDemo/kotlin/DemoPost01.kt)

![DemoPost01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoPost01Kt.png)

### DemoSpectralBlend01
[source code](src/jvmDemo/kotlin/DemoSpectralBlend01.kt)

![DemoSpectralBlend01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoSpectralBlend01Kt.png)

# orx-fx

orx-fx contains ready-to-use effects, or filters, for OPENRNDR. The provided filters are based on OPENRNDR's [`Filter` class](https://api.openrndr.org/org.openrndr.draw/-filter/index.html) and most will have [orx-parameters](https://github.com/openrndr/orx/tree/master/orx-parameters) annotations such that they can easily be used in combination with tools such as [orx-gui](https://github.com/openrndr/orx/tree/master/orx-gui).

All filters provided by orx-fx assume pre-multiplied alpha inputs, which is OPENRNDR's default.

## Effects index

Here's a (potentially incomplete) list of the effects provded by orx-fx.

### Anti-alias

 * `FXAA`, fast approximate anti-aliasing.
 
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
 - `GaussianBlur`, a slow but precise Gaussian blur
 - `HashBlur`, a noisy blur effect
 - `ZoomBlur`, a directional blur with a zooming effect


### Color
 - `ChromaticAberration`, a chromatic aberration effect based on RGB color separation
 - `ColorCorrection`, corrections for brightness, contrast, saturation and hue
 - `ColorLookup`, Color LUT filter
 - `ColorMix`, filter implementation of OPENRNDR's color matrix mixing
 - `LumaMap`, maps luminosity to two colors
 - `LumaOpacity`, maps luminosity to opacity but retains source color
 - `LumaThreshold`, applies a treshold on the input luminosity and maps to two colors
 - `Sepia`, applies a reddish-brown monochrome tint that imitates an old photograph
 - `SubtractConstant`, subtract a constant color from the source color

### Distortion

All distortion effects are opacity preserving

 - `BlockRepeat` - repeats a single configurable block of the source input
 - `StackRepeat` - repeats the source input in a stack fashion
 - `HorizontalWave` - applies a horizontal wave effect on the source input
 - `VerticalWave` - applies a vertical wave effect on the source input
 - `PerspectivePlane` - applies a planar perspective distortion on the source input
 
### Dither
 - `ADither` - a selection of dithering effects
 - `CMYKHalftone` - a configurable CMYK halftoning effect
 
 ### Edges
  - `LumaSobel` - A Sobel-kernel based luminosity edge detector
  - `EdgesWork` - An edges filter doubling as erosion
  
 ### Grain
  - `FilmGrain` - adds film-like grain to the source input
  
 ### Shadow
  - `DropShadow` - adds a drop shadow based on the opacity in the input image
  
 ### Tonemap
  - `Uncharted2Tonemap` - implements the Uncharted2 tonemapper
 
 ### Transform
  - `FlipVertically` - flips the source input vertically.
 
 
<!-- __demos__ >
# Demos
[DemoFluidDistort01Kt](src/demo/kotlin/DemoFluidDistort01Kt.kt
![DemoFluidDistort01Kt](https://github.com/openrndr/orx/blob/media/orx-fx/images/DemoFluidDistort01Kt.png
<!-- __demos__ -->
## Demos
### DemoFluidDistort01
[source code](src/demo/kotlin/DemoFluidDistort01.kt)

![DemoFluidDistort01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoFluidDistort01Kt.png)

### DemoLaserBlur01
[source code](src/demo/kotlin/DemoLaserBlur01.kt)

![DemoLaserBlur01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-fx/images/DemoLaserBlur01Kt.png)

# orx-color

Color spaces, palettes, histograms, named colors.

## Color presets

orx-color adds an extensive list of preset colors to `ColorRGBa`. Check [sources](src/commonMain/kotlin/presets/Colors.kt) for a listing of the preset colors.

## Color histograms

orx-color comes with tools to calculate color histograms for images. 

```kotlin
val histogram = calculateHistogramRGB(image)
val colors = histogram.sortedColors()
```

## Color sequences

Easy ways of creating blends between colors.

Using the `rangeTo` operator:
```kotlin
for (c in ColorRGBa.PINK..ColorRGBa.BLUE.toHSVa() blend 10) {
    drawer.fill = c
    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
    drawer.translate(0.0, 40.0)
}
```

Or blends for multiple color stops using `colorSequence`. Blending takes place in the colorspace of the input arguments.
```kotlin
val cs = colorSequence(0.0 to ColorRGBa.PINK,
        0.5 to ColorRGBa.BLUE,
        1.0 to ColorRGBa.PINK.toHSLUVa()) // <-- note this one is in hsluv

for (c in cs blend (width / 40)) {
    drawer.fill = c
    drawer.stroke = null
    drawer.rectangle(0.0, 0.0, 40.0,  height.toDouble())
    drawer.translate(40.0, 0.0)
}
```



## HSLUVa and HPLUVa colorspaces

Two color spaces are added: `ColorHSLUVa` and `ColorHPLUVa`, they are an implementation of the colorspaces presented at [hsluv.org](http://www.hsluv.org)
<!-- __demos__ -->
## Demos
### colormap/DemoSpectralZucconiColormap



![colormap-DemoSpectralZucconiColormapKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormap.kt)

### colormap/DemoSpectralZucconiColormapPhrase



![colormap-DemoSpectralZucconiColormapPhraseKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapPhraseKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormapPhrase.kt)

### colormap/DemoSpectralZucconiColormapPlot



![colormap-DemoSpectralZucconiColormapPlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapPlotKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormapPlot.kt)

### colormap/DemoTurboColormap



![colormap-DemoTurboColormapKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormap.kt)

### colormap/DemoTurboColormapPhrase



![colormap-DemoTurboColormapPhraseKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapPhraseKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormapPhrase.kt)

### colormap/DemoTurboColormapPlot



![colormap-DemoTurboColormapPlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapPlotKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormapPlot.kt)

### colormatrix/DemoColorMatrix01

This demo modifies the displayed image in each grid cell
using color matrix transformations to demonstrate color channel inversions based on
the grid cell's index. The image is adjusted to fit within each grid cell while maintaining
alignment.

Functionality:
- Loads an image from the specified file path.
- Splits the drawing area into an evenly spaced 4x2 grid.
- Applies different color matrix inversions (red, green, blue) based on the position index.
- Fits the image into each grid cell while providing horizontal alignment adjustments.

![colormatrix-DemoColorMatrix01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix01Kt.png)

[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix01.kt)

### colormatrix/DemoColorMatrix02

This demo modifies the displayed image in each grid cell
using color matrix transformations to demonstrate color channel inversions based on
the grid cell's index. The image is adjusted to fit within each grid cell while maintaining
alignment.

Functionality:
- Loads an image from the specified file path.
- Splits the drawing area into an evenly spaced 4x2 grid.
- Applies different color matrix inversions (red, green, blue) based on the position index.
- Fits the image into each grid cell while providing horizontal alignment adjustments.

![colormatrix-DemoColorMatrix02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix02Kt.png)

[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix02.kt)

### colormatrix/DemoColorMatrix03

Entry point for an application demonstrating the use of color matrix transformations on an image.

The program initializes a graphical application with a resolution of 720x720 pixels
and processes an image to display it in a series of grid cells, applying a hue shift
transformation based on the index of each cell.

Key features:
- Loads an image from a specified file path.
- Configures the drawing area to consist of a horizontal grid with 16 cells.
- Applies a color tint transformation utilizing the red channel, shifting its hue progressively
per cell index to create a colorful gradient effect.
- Adjusts the positions of the images within each grid cell for aesthetic alignment.

![colormatrix-DemoColorMatrix03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix03Kt.png)

[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix03.kt)

### colormatrix/DemoColorMatrix04

Entry point of a graphical application that demonstrates the use of color matrix
transformations on an image displayed within a grid layout.

Overview:
- Initializes a window with a resolution of 720x720 pixels.
- Loads an image from the specified file path.
- Splits the drawing canvas into a 7x1 grid of cells.
- In each grid cell, applies custom grayscale transformations to the image using
a color matrix. The grayscale transformation coefficients for red, green, and blue
channels are computed based on the index of the grid cell.
- Displays the adjusted image in each grid cell with horizontal alignment modifications
to position the images dynamically based on their index within the grid.

![colormatrix-DemoColorMatrix04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix04Kt.png)

[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix04.kt)

### colorRange/DemoColorRange01



![colorRange-DemoColorRange01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange01Kt.png)

[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange01.kt)

### colorRange/DemoColorRange02



![colorRange-DemoColorRange02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange02Kt.png)

[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange02.kt)

### colorRange/DemoColorRange03



![colorRange-DemoColorRange03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange03Kt.png)

[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange03.kt)

### colorRange/DemoColorRange04



![colorRange-DemoColorRange04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange04Kt.png)

[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange04.kt)

### DemoColorPalette01

Demonstrates the creation of color palettes using various available methods

![DemoColorPalette01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPalette01Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorPalette01.kt)

### DemoColorPalette02

By default, generated palettes contain colors of varying hue
but similar brightness and saturation.
Here we alter the brightness of each color using .shade() for
an increased dynamic range.

![DemoColorPalette02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPalette02Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorPalette02.kt)

### DemoColorPlane01

Visualizes a plane of ColorOKLCH colors as small 3D spheres
inside a 3D box. The plane represents all available hues and chromas.
The luminosity used to create the colors is modulated over time
with a slow sine wave.
Instanced rendering is used to render 90 x 100 colored spheres,
each with a unique position based on the RGB components of the color.

Since the OKLCH color space is larger than the RGB space, some
spheres would be outside the 3D box, but they are
actually clipped to the walls.

![DemoColorPlane01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPlane01Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorPlane01.kt)

### DemoColorPlane02



![DemoColorPlane02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPlane02Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorPlane02.kt)

### DemoColorSequence01

A demo that demonstrates 3D objects with custom shading and color gradients.

The application setup involves:
- Configuring the application window dimensions.
- Creating a color gradient using `ColorSequence` and converting it to a `ColorBuffer` for shading purposes.
- Defining a 3D sphere mesh with specified resolution.

The rendering process includes:
- Setting up an orbital camera extension to provide an interactive 3D view.
- Applying a custom fragment shader with a palette-based shading style.
- Rendering a grid of 3D spheres, each transformed and rotated to create a dynamic pattern.

![DemoColorSequence01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorSequence01Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorSequence01.kt)

### DemoDeltaE



![DemoDeltaEKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoDeltaEKt.png)

[source code](src/jvmDemo/kotlin/DemoDeltaE.kt)

### DemoFettePalette01



![DemoFettePalette01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoFettePalette01Kt.png)

[source code](src/jvmDemo/kotlin/DemoFettePalette01.kt)

### DemoFettePalette02



![DemoFettePalette02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoFettePalette02Kt.png)

[source code](src/jvmDemo/kotlin/DemoFettePalette02.kt)

### DemoHSLUV01



![DemoHSLUV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV01Kt.png)

[source code](src/jvmDemo/kotlin/DemoHSLUV01.kt)

### DemoHSLUV02



![DemoHSLUV02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV02Kt.png)

[source code](src/jvmDemo/kotlin/DemoHSLUV02.kt)

### DemoHueTools01



![DemoHueTools01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHueTools01Kt.png)

[source code](src/jvmDemo/kotlin/DemoHueTools01.kt)

### DemoMixSpectral01



![DemoMixSpectral01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoMixSpectral01Kt.png)

[source code](src/jvmDemo/kotlin/DemoMixSpectral01.kt)

### DemoOKHSV01



![DemoOKHSV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoOKHSV01Kt.png)

[source code](src/jvmDemo/kotlin/DemoOKHSV01.kt)

### DemoXSLUV01



![DemoXSLUV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoXSLUV01Kt.png)

[source code](src/jvmDemo/kotlin/DemoXSLUV01.kt)

### histogram/DemoHistogram01

package histogram

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.statistics.calculateHistogramRGB

/*
Demonstrates how to generate a palette with the top 32 colors
of a loaded image, sorted by luminosity. The colors are displayed
as rectangles overlayed on top of the image.

![histogram-DemoHistogram01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram01Kt.png)

[source code](src/jvmDemo/kotlin/histogram/DemoHistogram01.kt)

### histogram/DemoHistogram02

package histogram

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.statistics.calculateHistogramRGB
import kotlin.math.pow

/*
Show the color histogram of an image using non-uniform weighting,
prioritizing bright colors.

![histogram-DemoHistogram02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram02Kt.png)

[source code](src/jvmDemo/kotlin/histogram/DemoHistogram02.kt)

### histogram/DemoHistogram03

package histogram

import org.openrndr.application
import org.openrndr.draw.loadImage
import org.openrndr.extra.color.statistics.calculateHistogramRGB

/*
Create a simple grid-like composition based on colors sampled from image.
The cells are 32 by 32 pixels in size and are filled with a random sample
taken from the color histogram of the image.

Note: due to its random nature the resulting animation contains flickering colors.

![histogram-DemoHistogram03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram03Kt.png)

[source code](src/jvmDemo/kotlin/histogram/DemoHistogram03.kt)

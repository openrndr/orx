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

This program demonstrates the `spectralZucconi6()` function, which
takes a normalized value and returns a `ColorRGBa` using the
accurate spectral colormap developed by Alan Zucconi.

It draws a varying number of vertical bands (between 16 and 48)
filled with various hues.

![colormap-DemoSpectralZucconiColormapKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormap.kt)

### colormap/DemoSpectralZucconiColormapPhrase

This program demonstrates how to use the shader-based version of
the `spectral_zucconi6()` function, which
takes a normalized value and returns an `rgb` color using the
accurate spectral colormap developed by Alan Zucconi.

It shades a full-window rectangle using its normalized `x` coordinate
in a `ShadeStyle` to choose pixel colors.

![colormap-DemoSpectralZucconiColormapPhraseKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapPhraseKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormapPhrase.kt)

### colormap/DemoSpectralZucconiColormapPlot

This demo uses the shader based `spectral_zucconi6()` function to fill the background,
then visualizes the red, green and blue components of the colors used in the background
as red, green and blue line strips.

The Vector2 points for the line strips are calculated only once when the program starts.

![colormap-DemoSpectralZucconiColormapPlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapPlotKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormapPlot.kt)

### colormap/DemoTurboColormap

This program demonstrates the `turboColormap()` function, which
takes a normalized value and returns a `ColorRGBa` using the
Turbo colormap developed by Google.

It draws a varying number of vertical bands (between 16 and 48)
filled with various hues.

![colormap-DemoTurboColormapKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormap.kt)

### colormap/DemoTurboColormapPhrase

This program demonstrates how to use the shader-based version of
the `turbo_colormap()` function, which
takes a normalized value and returns an `rgb` color using the
Turbo colormap developed by Google.

It shades a full-window rectangle using its normalized `x` coordinate
in a `ShadeStyle` to choose pixel colors.

![colormap-DemoTurboColormapPhraseKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapPhraseKt.png)

[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormapPhrase.kt)

### colormap/DemoTurboColormapPlot

This demo uses the shader based `turbo_colormap()` function to fill the background,
then visualizes the red, green and blue components of the colors used in the background
as red, green and blue line strips.

The Vector2 points for the line strips are calculated only once when the program starts.

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

Comparison of color lists generated by interpolating from
`PINK` to `BLUE` in six different color spaces.

![colorRange-DemoColorRange01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange01Kt.png)

[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange01.kt)

### colorRange/DemoColorRange02

Demonstrates how to create a `ColorSequence` containing three colors, one of them in the HSLUV color space.

Each color in the sequence is assigned a normalized position: in this program, one at the start (0.0),
one in the middle (0.5) and one at the end (1.0).

The `ColorSpace.blend()` method is used to get a list with 18 interpolated `ColorRGBa` colors,
then those colors are drawn as vertical rectangles covering the whole window.

![colorRange-DemoColorRange02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange02Kt.png)

[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange02.kt)

### colorRange/DemoColorRange03

This program creates color interpolations from `ColorRGBa.BLUE` to
`ColorRGBa.PINK` in 25 steps in multiple color spaces.

The window height is adjusted based on the number of interpolations to show.

The resulting gradients differ in saturation and brightness and apparently include more
`BLUE` or more `PINK` depending on the chosen color space.

![colorRange-DemoColorRange03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange03Kt.png)

[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange03.kt)

### colorRange/DemoColorRange04

A visualization of color interpolations inside a 3D RGB cube with an interactive 3D `Orbital` camera.

The hues of the source and target colors are animated over time.

The color interpolations are shown simultaneously in nine different color spaces, revealing how in
each case they share common starting and ending points in 3D, but have unique paths going from
start to end.

By rotating the cube 90 degrees towards the left and slightly zooming out, one can appreciate how
one of the points moves along the edges of the cube, while the other moves on the edges of a
smaller, invisible cube.


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

Demonstrates `generateColorRamp()`, a function with numerous parameters to generate color ramps.

The first argument is the number of base colors to produce.

Two other arguments are set based on the mouse x and y coordinates,
letting the user affect the hue interactively.

The created ramp contains `baseColors`, `lightColors` and `darkColors`. All three collections
are rendered as small colored rectangles.

In the center of the window, four colors from those collections are rendered as larger rectangles,
using a random base color, a random light color, and two random dark colors.

![DemoFettePalette01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoFettePalette01Kt.png)

[source code](src/jvmDemo/kotlin/DemoFettePalette01.kt)

### DemoFettePalette02



![DemoFettePalette02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoFettePalette02Kt.png)

[source code](src/jvmDemo/kotlin/DemoFettePalette02.kt)

### DemoHSLUV01

Interactive program comparing the HSLUV and the HSL color spaces.

The program draws two series of rotated rectangles with hues increasing
from 0 to 360 in steps of 12 degrees. The saturation depends on the horizontal
mouse position; the luminosity on its vertical position.

By exploring various saturation and luminosity values, one can appreciate
whether adjacent colors are more similar in HSLUV or in HSL.

![DemoHSLUV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV01Kt.png)

[source code](src/jvmDemo/kotlin/DemoHSLUV01.kt)

### DemoHSLUV02

Visualizes the HSLUV color space by drawing a phyllotaxis pattern.

The program also demonstrates how to create a function that returns a `sequence`.
Unlike collections, sequences don't contain elements, they produce them while iterating.
https://kotlinlang.org/docs/sequences.html

Each position in the phyllotaxis is rendered as a spherical gradient by repeatedly drawing
each circle with different sizes and a fill color.

![DemoHSLUV02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV02Kt.png)

[source code](src/jvmDemo/kotlin/DemoHSLUV02.kt)

### DemoHueTools01



![DemoHueTools01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHueTools01Kt.png)

[source code](src/jvmDemo/kotlin/DemoHueTools01.kt)

### DemoMixSpectral01



![DemoMixSpectral01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoMixSpectral01Kt.png)

[source code](src/jvmDemo/kotlin/DemoMixSpectral01.kt)

### DemoOKHSV01

Shows the color green shifted in hue over 360 degrees in 36 steps, side by side in 4 color spaces:
OKHSV, HSV, HSL and OKHSL.

To shift hues the method `shiftHue()` is applied. The resulting colors are then converted
from each color space to RGB so they can be used for drawing.

![DemoOKHSV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoOKHSV01Kt.png)

[source code](src/jvmDemo/kotlin/DemoOKHSV01.kt)

### DemoXSLUV01

Visualize the XSLUV color space by drawing a recursively subdivided set of arcs.

The provided `Arc` class provides a `contour` getter, which creates a "thick" arc with
its thickness defined by the `height` argument. This is created by two arcs and two
connecting lines.

The mouse x coordinate controls the saturation, while the y coordinate controls the luminosity.
The two if-statements check whether the program is taking a screenshot (this happens when
it runs on GitHub actions) to set fixed saturation and luminosity values.

![DemoXSLUV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoXSLUV01Kt.png)

[source code](src/jvmDemo/kotlin/DemoXSLUV01.kt)

### histogram/DemoHistogram01

Demonstrates how to generate a palette with the top 32 colors
of a loaded image, sorted by luminosity. The colors are displayed
as rectangles overlayed on top of the image.

![histogram-DemoHistogram01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram01Kt.png)

[source code](src/jvmDemo/kotlin/histogram/DemoHistogram01.kt)

### histogram/DemoHistogram02

Show the color histogram of an image using non-uniform weighting,
prioritizing bright colors.

![histogram-DemoHistogram02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram02Kt.png)

[source code](src/jvmDemo/kotlin/histogram/DemoHistogram02.kt)

### histogram/DemoHistogram03

Create a simple grid-like composition based on colors sampled from image.
The cells are 32 by 32 pixels in size and are filled with a random sample
taken from the color histogram of the image.

Note: due to its random nature the resulting animation contains flickering colors.

![histogram-DemoHistogram03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram03Kt.png)

[source code](src/jvmDemo/kotlin/histogram/DemoHistogram03.kt)

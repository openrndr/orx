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
[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormap.kt)

![colormap-DemoSpectralZucconiColormapKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapKt.png)

### colormap/DemoSpectralZucconiColormapPhrase
[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormapPhrase.kt)

![colormap-DemoSpectralZucconiColormapPhraseKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapPhraseKt.png)

### colormap/DemoSpectralZucconiColormapPlot
[source code](src/jvmDemo/kotlin/colormap/DemoSpectralZucconiColormapPlot.kt)

![colormap-DemoSpectralZucconiColormapPlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoSpectralZucconiColormapPlotKt.png)

### colormap/DemoTurboColormap
[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormap.kt)

![colormap-DemoTurboColormapKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapKt.png)

### colormap/DemoTurboColormapPhrase
[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormapPhrase.kt)

![colormap-DemoTurboColormapPhraseKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapPhraseKt.png)

### colormap/DemoTurboColormapPlot
[source code](src/jvmDemo/kotlin/colormap/DemoTurboColormapPlot.kt)

![colormap-DemoTurboColormapPlotKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormap-DemoTurboColormapPlotKt.png)

### colormatrix/DemoColorMatrix01
[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix01.kt)

![colormatrix-DemoColorMatrix01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix01Kt.png)

### colormatrix/DemoColorMatrix02
[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix02.kt)

![colormatrix-DemoColorMatrix02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix02Kt.png)

### colormatrix/DemoColorMatrix03
[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix03.kt)

![colormatrix-DemoColorMatrix03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix03Kt.png)

### colormatrix/DemoColorMatrix04
[source code](src/jvmDemo/kotlin/colormatrix/DemoColorMatrix04.kt)

![colormatrix-DemoColorMatrix04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colormatrix-DemoColorMatrix04Kt.png)

### colorRange/DemoColorRange01
[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange01.kt)

![colorRange-DemoColorRange01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange01Kt.png)

### colorRange/DemoColorRange02
[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange02.kt)

![colorRange-DemoColorRange02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange02Kt.png)

### colorRange/DemoColorRange03
[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange03.kt)

![colorRange-DemoColorRange03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange03Kt.png)

### colorRange/DemoColorRange04
[source code](src/jvmDemo/kotlin/colorRange/DemoColorRange04.kt)

![colorRange-DemoColorRange04Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/colorRange-DemoColorRange04Kt.png)

### DemoColorPalette01
[source code](src/jvmDemo/kotlin/DemoColorPalette01.kt)

![DemoColorPalette01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPalette01Kt.png)

### DemoColorPalette02
[source code](src/jvmDemo/kotlin/DemoColorPalette02.kt)

![DemoColorPalette02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPalette02Kt.png)

### DemoColorPlane01
[source code](src/jvmDemo/kotlin/DemoColorPlane01.kt)

![DemoColorPlane01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPlane01Kt.png)

### DemoColorPlane02
[source code](src/jvmDemo/kotlin/DemoColorPlane02.kt)

![DemoColorPlane02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorPlane02Kt.png)

### DemoColorSequence01
[source code](src/jvmDemo/kotlin/DemoColorSequence01.kt)

![DemoColorSequence01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoColorSequence01Kt.png)

### DemoDeltaE
[source code](src/jvmDemo/kotlin/DemoDeltaE.kt)

![DemoDeltaEKt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoDeltaEKt.png)

### DemoFettePalette01
[source code](src/jvmDemo/kotlin/DemoFettePalette01.kt)

![DemoFettePalette01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoFettePalette01Kt.png)

### DemoFettePalette02
[source code](src/jvmDemo/kotlin/DemoFettePalette02.kt)

![DemoFettePalette02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoFettePalette02Kt.png)

### DemoHSLUV01
[source code](src/jvmDemo/kotlin/DemoHSLUV01.kt)

![DemoHSLUV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV01Kt.png)

### DemoHSLUV02
[source code](src/jvmDemo/kotlin/DemoHSLUV02.kt)

![DemoHSLUV02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV02Kt.png)

### DemoHueTools01
[source code](src/jvmDemo/kotlin/DemoHueTools01.kt)

![DemoHueTools01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHueTools01Kt.png)

### DemoMixSpectral01
[source code](src/jvmDemo/kotlin/DemoMixSpectral01.kt)

![DemoMixSpectral01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoMixSpectral01Kt.png)

### DemoOKHSV01
[source code](src/jvmDemo/kotlin/DemoOKHSV01.kt)

![DemoOKHSV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoOKHSV01Kt.png)

### DemoXSLUV01
[source code](src/jvmDemo/kotlin/DemoXSLUV01.kt)

![DemoXSLUV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoXSLUV01Kt.png)

### histogram/DemoHistogram01
[source code](src/jvmDemo/kotlin/histogram/DemoHistogram01.kt)

![histogram-DemoHistogram01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram01Kt.png)

### histogram/DemoHistogram02
[source code](src/jvmDemo/kotlin/histogram/DemoHistogram02.kt)

![histogram-DemoHistogram02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram02Kt.png)

### histogram/DemoHistogram03
[source code](src/jvmDemo/kotlin/histogram/DemoHistogram03.kt)

![histogram-DemoHistogram03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/histogram-DemoHistogram03Kt.png)

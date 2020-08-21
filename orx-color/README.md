# orx-color

Tools to work with color

## Color presets

orx-color adds an extensive list of preset colors to `ColorRGBa`. Check [sources](src/main/kotlin/presets/Colors.kt) for a listing of the preset colors.

## Color histograms

orx-color comes with tools to calculate color histograms for images. 

```
val histogram = calculateHistogramRGB(image)
val colors = histogram.sortedColors()
```

## Color sequences

Easy ways of creating blends between colors.

Using the `rangeTo` operator:
```
for (c in ColorRGBa.PINK..ColorRGBa.BLUE.toHSVa() blend 10) {
    drawer.fill = c
    drawer.rectangle(0.0, 0.0, 40.0, 40.0)
    drawer.translate(0.0, 40.0)
}
```

Or blends for multiple color stops using `colorSequence`. Blending takes place in the colorspace of the input arguments.
```
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
### DemoHSLUV01
[source code](src/demo/kotlin/DemoHSLUV01.kt)

![DemoHSLUV01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV01Kt.png)

### DemoHSLUV02
[source code](src/demo/kotlin/DemoHSLUV02.kt)

![DemoHSLUV02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHSLUV02Kt.png)

### DemoHistogram01
[source code](src/demo/kotlin/DemoHistogram01.kt)

![DemoHistogram01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHistogram01Kt.png)

### DemoHistogram02
[source code](src/demo/kotlin/DemoHistogram02.kt)

![DemoHistogram02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHistogram02Kt.png)

### DemoHistogram03
[source code](src/demo/kotlin/DemoHistogram03.kt)

![DemoHistogram03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHistogram03Kt.png)

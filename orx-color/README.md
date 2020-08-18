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
<!-- __demos__ -->
## Demos
### DemoHistogram01
[source code](src/demo/kotlin/DemoHistogram01.kt)

![DemoHistogram01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHistogram01Kt.png)

### DemoHistogram02
[source code](src/demo/kotlin/DemoHistogram02.kt)

![DemoHistogram02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHistogram02Kt.png)

### DemoHistogram03
[source code](src/demo/kotlin/DemoHistogram03.kt)

![DemoHistogram03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-color/images/DemoHistogram03Kt.png)

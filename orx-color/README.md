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
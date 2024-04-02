# orx-svg

SVG reader and writer library.

## Methods

```kotlin
fun loadSVG(fileOrUrlOrSvg: String): Composition
fun loadSVG(file: File): Composition

fun parseSVG(svgString: String): Composition

fun Shape.toSvg(): String
fun ShapeContour.toSvg(): String

fun Composition.saveToFile(file: File)
fun Composition.toSVG(): String
```

Find basic examples of loading and saving SVG files 
[in the guide](https://guide.openrndr.org/drawing/drawingSVG.html).

_The code in `orx-svg` was previously found under `openrndr-svg` in the `openrndr` repository._

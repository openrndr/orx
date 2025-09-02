# orx-palette

Collections of color palettes and tools for interacting with them.

Find demos in the demo folder.

## ColorBrewer2

A collection of color palettes based on the research of Dr. Cynthia Brewer. Explore them live at [colorbrewer2.org](https://colorbrewer2.org/).

Each Palette has between 3 and 11 colors.

Use `colorBrewer2Palettes()` to query and obtain a list of `ColorBrewer2Palette` instances.

```kotlin
// all palettes
val palettes = colorBrewer2Palettes()

// palettes with 5 colors
val palettes = colorBrewer2Palettes(numberOfColors = 5)

// palettes of type Sequential 
val palettes = colorBrewer2Palettes(palettetype = ColorBrewer2Type.Sequential)
```

Once we have some palettes, we can pick one and use its colors:

```kotlin
palettes.first().colors.forEachIndexed { i, color ->
    drawer.fill = color
    drawer.circle(drawer.bounds.center, 300.0 - i * 40.0)
}
```

## Palette Studio

A class to load palette collections from JSON files, load random palettes and sort colors. JVM only.

### Usage

```kotlin
val paletteStudio = PaletteStudio(
    loadDefault = true, // Loads the first collection of palettes. [default -> true]
    sortBy = PaletteStudio.SortBy.DARKEST, // Sorts the colors by luminance. [default -> PaletteStudio.SortBy.NO_SORTING]
    collection = PaletteStudio.Collections.TWO, // Chooses which collection to load [default -> Collections.ONE]
    colorCountConstraint = 3 // Constraints the number of colors in the palette [default -> 0]
)

// The choice of the background and foreground colors is based on contrast ratios
drawer.background(paletteStudio.background)
drawer.stroke = paletteStudio.foreground

val randomPaletteColor = Random.pick(paletteStudio.colors!!)
val randomColorExcludingBackground = Random.pick(paletteStudio.colors2!!)

// grabs a random palette from the collection
paletteStudio.randomPalette()

// randomizes the order of the colors in the palette
paletteStudio.randomize()

// changes the collection of palettes
paletteStudio.loadCollection(PaletteStudio.Collections.TWO)

// load your own from a JSON file with a structure of Array<Array<String>>
paletteStudio.loadExternal("data/palette-autumn.json")
```

#### Keybindings

Keybindings for getting a random palette (`l`) and randomizing (`k`) one can be set easily by declaring inside the `program`:
```kotlin
val paletteStudio = PaletteStudio()

extend(paletteStudio)
```
<!-- __demos__ -->
## Demos
### DemoColorBrewer2_01

Demonstrates how to use a ColorBrewer2 palette.
Finds the first available palette with 5 colors,
then draws concentric circles filled with those colors.

![DemoColorBrewer2_01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-palette/images/DemoColorBrewer2_01Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorBrewer2_01.kt)

### DemoColorBrewer2_02

Visualizes the ColorBrewer2 color palettes with 8 colors as circles
made of colored arcs.

![DemoColorBrewer2_02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-palette/images/DemoColorBrewer2_02Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorBrewer2_02.kt)

### DemoColorBrewer2_03

Visualizes 49 ColorBrewer2 color palettes of type "Diverging" as circles
made of colored arcs. Since there are more palettes than grid cells,
not all palettes are visualized.

![DemoColorBrewer2_03Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-palette/images/DemoColorBrewer2_03Kt.png)

[source code](src/jvmDemo/kotlin/DemoColorBrewer2_03.kt)

### DemoPaletteStudio01

Demonstrates how to access palette colors using PaletteStudio.
A new random palette is loaded every 60 animation frames.

![DemoPaletteStudio01Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-palette/images/DemoPaletteStudio01Kt.png)

[source code](src/jvmDemo/kotlin/DemoPaletteStudio01.kt)

### DemoPaletteStudio02

Demonstrates how to create a design using PaletteStudio.
Press the `c` key to load a random palette.


![DemoPaletteStudio02Kt](https://raw.githubusercontent.com/openrndr/orx/media/orx-palette/images/DemoPaletteStudio02Kt.png)

[source code](src/jvmDemo/kotlin/DemoPaletteStudio02.kt)

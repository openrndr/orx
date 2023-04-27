# orx-palette

Provides hundreds of color palettes.

## Usage

```kotlin
import org.openrndr.extra.palette.PaletteStudio

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
paletteStudio.changeCollection(PaletteStudio.Collections.TWO)

// load your own from a JSON file with a structure of Array<Array<String>>
paletteStudio.loadExternal("data/palette-autumn.json")
```

### Keybindings

Keybindings for getting a random palette (`l`) and randomizing (`k`) one can be set easily by declaring inside the `program`:
```kotlin
val paletteStudio = PaletteStudio()

extend(paletteStudio)
```

## Example

```kotlin
fun main() = application {
    configure {
        title = "Palette"
        width = 720
        height = 720
    }
    program {
        val colors = mutableListOf<ColorRGBa>()

        fun fillColors() {
            for (n in 0..36) {
                when(n) {
                    12 -> paletteStudio.changeCollection(PaletteStudio.Collections.TWO)
                    24 -> paletteStudio.changeCollection(PaletteStudio.Collections.THREE)
                }

                val color = Random.pick(paletteStudio.colors!!)

                colors.add(color)
            }
        }

        keyboard.keyDown.listen {
            if (it.name == "c") {
                colors.clear()
                fillColors()
            }
        }

        fillColors()

        extend() {
            drawer.background(paletteStudio.background)

            val size = 120.0
            val radius = size / 2.0

            for (x in 0 until 6) {
                for (y in 0 until 6) {
                    val index = x + y * 6
                    val color = colors[index]
                    val x = size * x
                    val y = size * y

                    drawer.fill = color
                    drawer.stroke = color

                    if (index <= 11 || index > 23) {
                        drawer.circle(x + radius, y + radius, radius)
                    } else {
                        drawer.rectangle(x, y, size, size)
                    }
                }
            }
        }
    }
}
```

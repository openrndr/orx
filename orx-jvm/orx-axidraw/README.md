# orx-axidraw

GUI for configuring and plotting with an Axidraw pen-plotter.

Uses the [AxiCLI](https://axidraw.com/doc/cli_api/#introduction) command line tool 
to communicate with the pen plotter.

Requires: Python 3.8 or higher.

This orx create a Python virtual environment and downloads AxiCLI automatically. 

## Usage

```kotlin
fun main() = application {
    program {
        val axi = Axidraw(this, PaperSize.A5)
        axi.resizeWindow()

        val gui = WindowedGUI()
        gui.add(axi)

        axi.draw {
            fill = null
            axi.bounds.grid(4, 6).flatten().forEach { 
                circle(it.center, Double.uniform(20.0, 50.0))
            }
        }
        
        extend(gui)
        extend {
            drawer.clear(ColorRGBa.WHITE)
            axi.display(drawer)
        }
    }
}
```

Study the inputs available in the GUI. Most are explained in the [AxiCLI](https://axidraw.com/doc/cli_api/#introduction) documentation page.

### Important

* Choose the correct pen-plotter model and servo type in the GUI before plotting.
* Always make sure the pen is at the home position before starting to plot. If it's not, unpower the steppers,
drag the carriage home (near the Axidraw's CPU), then power the steppers back on.

### Tips

* One can repeatedly click on `toggle up/down` and adjust `pen pos down` and `pen pos up`
to find the ideal heights for the pen.
* Enable `fills occlude strokes` and increase margin value to hide elements near 
the borders of the paper.
* Click `save` to save your SVG file.
* Click `plot` to plot the visible design using the current settings.
* A [2D camera](https://guide.openrndr.org/extensions/camera2D.html) is enabled by default to place your design on the paper.
* Click `resume plotting` after pressing the hardware pause button (or including a pause
command on a layer) to continue.
* To get a plotting time estimate, enable `preview` and click `plot`. Nothing will be plotted, but the estimate will be shown in the IDE console.

The `Load` and `Save` buttons *at the top of the GUI* can be used to load and save the plotting settings. In a future version we may embed the plotting settings into the SVG file.

### Multi color plots

orx-axidraw makes it easy to create multi-pen plots. To do that, use two or more stroke colors in your design. The order of the lines does not matter. Then, before plotting, call `axi.groupStrokeColors()`. This will group curves into layers based on their stroke colors and insert a pause between layers, allowing you to change the pen. 

When the plotter pauses during plotting, change the pen and click `resume plotting` to continue.

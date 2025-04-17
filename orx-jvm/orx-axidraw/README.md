# orx-axidraw

GUI for configuring an plotting with an Axidraw pen-plotter.

Uses the [AxiCLI](https://axidraw.com/doc/cli_api/#introduction) command line tool 
to communicate with the pen plotter.

NOTE: Requires Python 3.8 or higher.

NOTE: work in progress, Linux-only alpha release expecting `axicli` to be in the path.


## Usage

```kotlin
fun main() = application {
    program {
        val axi = Axidraw(PaperSize.A5)
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

Study the inputs available in the GUI. Most are explained in the AxiCLI documentation page.

Choose the correct pen-plotter model and servo type before plotting.

One can repeatedly click on `toggle up/down` and adjust `pen pos down` and `pen pos up`
to find the ideal heights for the pen.

Enable `fills occlude strokes` and increase margin value to hide elements near 
the borders of the paper.

Click `save` to save your SVG file.
Click `plot` to plot the visible design using the current settings.
A [2D camera](https://guide.openrndr.org/extensions/camera2D.html) is enabled by default 
to place your design on the paper.

Click `resume plotting` after pressing the hardware pause button (or including a pause
command on a layer) to continue.

If `preview` is enabled when plotting a plotting-time estimate will be shown.

The `Load` and `Save` buttons at the top can be used to load and save the plotting settings.

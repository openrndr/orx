import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.parameters.ActionParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.ListParameter
import org.openrndr.extra.parameters.MutableListParameter
import org.openrndr.extra.textwriter.writer
import org.openrndr.panel.collections.SelectableList
import org.openrndr.panel.collections.SelectableMutableList
import org.openrndr.shape.Rectangle

/**
 * Demonstrates the use of the [ListParameter] and [MutableListParameter] annotations to
 * show lists and mutable lists in a drop-down in a GUI.
 *
 * Shows how to set the selectedIndex of the drop-down, how to add items to a [SelectableMutableList]
 * and how to delete specific entries in them.
 */
fun main() = application {
    configure {
        width = 720
        height = 360
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 40.0)
        val gui = GUI()
        val settings = @Description("Settings") object {
            @ListParameter("Fruit", order = 10)
            var fruits = SelectableList(listOf("apple", "orange"), selectedIndex = 0)

            @MutableListParameter("Amount", order = 20)
            var amounts = SelectableMutableList(listOf(10, 20, 30), 1)

            @ActionParameter("Add", order = 30)
            fun add() {
                amounts.add(Int.uniform(1, 99))
                amounts.selectedIndex = amounts.lastIndex
            }

            @ActionParameter("Delete first", order = 40)
            fun deleteFirst() {
                if (amounts.isNotEmpty()) amounts.removeAt(0)
            }

            @ActionParameter("Delete selected", order = 45)
            fun deleteSelected() {
                if (amounts.isNotEmpty()) {
                    amounts.removeAt(amounts.selectedIndex)
                }
            }

            @ActionParameter("Select last", order = 50)
            fun selectLast() {
                amounts.selectedIndex = amounts.lastIndex
            }
        }

        gui.compartmentsCollapsedByDefault = false
        gui.add(settings)
        gui.onChange { name, value -> println("$name: $value") }
        extend(gui)
        extend {
            drawer.clear(ColorRGBa.PINK)
            drawer.stroke = ColorRGBa.WHITE
            drawer.fill = null
            drawer.circle(drawer.bounds.center, (settings.amounts.selected ?: 0) * 4.0)

            drawer.fill = ColorRGBa.WHITE
            drawer.fontMap = font
            writer {
                box = Rectangle(220.0, 50.0, width - 250.0, height - 100.0)
                text(settings.fruits.selected ?: "-")
                newLine()
                text(settings.amounts.selected?.toString() ?: "-")
            }
        }
    }
}

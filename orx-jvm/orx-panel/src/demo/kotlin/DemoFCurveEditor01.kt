import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.fcurve.fcurve
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import kotlin.random.Random

/**
 * Demonstrates the editor of fcurves on a long curve of 600 keys over two minutes, with jumps here and there. The
 * view zooms horizontally with the wheel, vertically with shift and the wheel, and F frames the whole curve. The
 * tools: select and move keys and handles (V), add keys with the pen (P), draw the curve by hand (D), cut the curve
 * (C), break it into a jump (B), and sculpt it under a brush with grab (G), smooth (S), flatten (L) and noise (N);
 * [ and ] change the radius of the brush. Simplify (R) fits the curve between the selected keys again with fewer
 * segments, like after sculpting. Clear removes the curve, to build a new one with the pen or by drawing; the context
 * menu makes the selected keys smooth, linear or constant. The circle above the editor follows the curve at the
 * playhead, and mod+z undoes the edits.
 */
fun main() = application {
    configure {
        width = 1280
        height = 720
        windowResizable = true
    }
    program {
        val random = Random(7)
        val curve = fcurve {
            moveTo(0.0)
            var y = 0.0
            for (i in 0 until 600) {
                // a jump every now and then
                if (i % 97 == 96) moveTo(random.nextDouble(-1.0, 1.0))
                y = (y + random.nextDouble(-0.4, 0.4)).coerceIn(-1.0, 1.0)
                continueTo(random.nextDouble(0.1, 0.3), y)
            }
        }
        lateinit var editor: FCurveEditor
        val buttons = mutableMapOf<FCurveTool, Button>()

        val cm = controlManager {
            css("""
                .page { display: flex; flex-direction: column; align-items: stretch; width: 100%; height: 100%; padding: 8px }
                .preview { height: 140px; flex-shrink: 0 }
                .toolbar { display: flex; flex-direction: row; align-items: center; flex-shrink: 0 }
                .toolbar > button { width: auto }
                .toolbar > button.active { background-color: #5ab0ff; color: #101216 }
                .toolbar > slider { width: 200px }
                fcurve-editor { flex: 1; flex-basis: 0; height: auto }
            """)
            layout {
                div("page") {
                    div("preview") { }
                    div("toolbar") {
                        for (t in FCurveTool.entries) {
                            buttons[t] = button {
                                label = "${t.title} (${t.key.uppercase()})"
                                events.clicked.listen { editor.tool = t }
                            }
                        }
                        button {
                            label = "Clear"
                            events.clicked.listen { editor.clear() }
                        }
                        button {
                            label = "Simplify (R)"
                            events.clicked.listen { editor.simplifySelection() }
                        }
                        slider {
                            label = "radius"; range = Range(8.0, 400.0); precision = 0; value = 60.0
                            events.valueChanged.listen { editor.brushRadius = it.newValue }
                        }
                        slider {
                            label = "strength"; range = Range(0.0, 1.0); precision = 2; value = 0.5
                            events.valueChanged.listen { editor.brushStrength = it.newValue }
                        }
                    }
                    editor = fcurveEditor { value = curve }
                }
            }
        }
        // the button of the tool is active
        fun showTool(tool: FCurveTool) = buttons.forEach { (t, b) -> if (t == tool) b.classes.add(ElementClass("active")) else b.classes.remove(ElementClass("active")) }
        editor.toolChanged.listen(::showTool)
        showTool(editor.tool)

        // the circle that the curve moves
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            val c = editor.value
            // an empty curve has no time to play
            if (c.duration <= 0.0) {
                editor.playhead = null
                return@extend
            }
            val t = c.start + (seconds * 4.0) % c.duration
            editor.playhead = t
            val v = c.value(t)
            drawer.fill = ColorRGBa.fromHex("#f88379")
            drawer.stroke = null
            drawer.circle(80.0 + (width - 160.0) * (t - c.start) / c.duration, 78.0 - v * 50.0, 10.0)
        }
        extend(cm)
    }
}

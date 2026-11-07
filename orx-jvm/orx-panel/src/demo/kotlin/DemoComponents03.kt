import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import kotlin.math.sin

/**
 * Demonstrates more tools of panels, in a dock: a node editor of a small graph, a text area with the code of a shader
 * in highlighted GLSL, a color editor with a wheel in OKLCH, swatches and a pipette that picks colors from the window,
 * and live plots of the frame time and of values, a histogram and a scope. The edits are undone with mod+z, and the
 * menus are used with the keyboard: F10, then the arrow keys.
 */
fun main() = application {
    configure {
        width = 1200
        height = 760
        windowResizable = true
    }
    program {
        var color = ColorRGBa.fromHex("#f88379")
        var last = System.nanoTime()

        extend { drawer.clear(ColorRGBa.fromHex("#16181c")) }

        val cm = controlManager {
            css("""
                .page { display: flex; flex-direction: column; align-items: stretch; width: 100%; height: 100% }
                .page > dock { flex: 1; flex-basis: 0 }
                dock-content { background-color: #1d2026 }
                .graph { padding: 0 }
                .graph > node-editor { height: 100% }
                .shader > text-area { height: 100%; margin: 0 }
                .shader { padding: 0 }
            """)
            layout {
                div("page") {
                    menuBar {
                        menu("Graph") {
                            submenu("Add") {
                                for (kind in listOf("noise", "blur", "mix", "output")) item(kind) {}
                            }
                        }
                    }
                    dock {
                        val graph = panel("Graph") {
                            nodeEditor {
                                val noise = add(GraphNode("noise", outputs = listOf(Port("image", "image"))), Vector2(40.0, 60.0))
                                val gradient = add(GraphNode("gradient", outputs = listOf(Port("image", "image"))), Vector2(40.0, 200.0))
                                val blur = add(GraphNode("blur", listOf(Port("image", "image"), Port("radius", "number")), listOf(Port("image", "image"))), Vector2(260.0, 60.0))
                                val mix = add(GraphNode("mix", listOf(Port("a", "image"), Port("b", "image"), Port("t", "number")), listOf(Port("image", "image"))), Vector2(480.0, 120.0))
                                add(GraphNode("time", outputs = listOf(Port("seconds", "number"))), Vector2(260.0, 260.0))
                                add(GraphNode("output", listOf(Port("image", "image"))), Vector2(700.0, 140.0))
                                connect(noise, 0, blur, 0)
                                connect(blur, 0, mix, 0)
                                connect(gradient, 0, mix, 1)
                                // a double click on the background adds a node there
                                events.requested.listen { p -> add(GraphNode("node ${nodes.size + 1}", listOf(Port("in")), listOf(Port("out"))), p) }
                            }
                        }.also { it.content.classes.add(ElementClass("graph")) }
                        panel("Shader", DockZone.CENTER, graph) {
                            textArea {
                                highlighter = Highlighters.glsl
                                value = """
                                    |// the fragment of a shade style
                                    |uniform float p_time;
                                    |
                                    |void main() {
                                    |    vec2 uv = c_boundsPosition.xy;
                                    |    float d = length(uv - vec2(0.5));
                                    |    vec3 color = mix(vec3(0.35, 0.69, 1.0), vec3(0.97, 0.51, 0.47), smoothstep(0.1, 0.5, d));
                                    |    x_fill = vec4(color * (0.8 + 0.2 * sin(p_time * 3.0)), 1.0);
                                    |}
                                """.trimMargin()
                            }
                        }.also { it.content.classes.add(ElementClass("shader")) }
                        val colors = panel("Color", DockZone.RIGHT, graph) {
                            colorEditor { value = color }.valueChanged.listen { color = it.newValue }
                        }
                        panel("Plots", DockZone.BOTTOM, graph) {
                            plot {
                                range = 0.0..40.0
                                watch("frame ms") {
                                    val now = System.nanoTime()
                                    ((now - last) / 1E6).also { last = now }
                                }
                            }
                            plot {
                                watch("sin") { sin(seconds * 2.0) }
                                watch("sin²") { sin(seconds * 3.1).let { it * it } }
                            }
                            histogram {
                                bins = 40
                                range = -3.0..3.0
                                val random = java.util.Random(1)
                                samples = DoubleArray(5000) { random.nextGaussian() }
                            }
                            scope {
                                samples = FloatArray(2048) { i -> (sin(i * 0.05) * 0.6 + sin(i * 0.31) * 0.25).toFloat() }
                            }
                        }
                        select(colors)
                    }
                }
            }
        }
        extend(cm)
    }
}

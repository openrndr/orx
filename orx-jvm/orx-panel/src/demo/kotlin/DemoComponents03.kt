import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*
import org.openrndr.shape.Rectangle
import kotlin.math.sin

/**
 * Demonstrates more tools of panels, in a dock: a node editor of a small graph, with ports colored by their types,
 * a live value on a port, a node with a body that draws a wave, a slider in the body of the mix node, and a status that
 * tells the output has no input; Graph > Compact nodes shows only the connected ports, and the wire from the time to
 * the radius of the blur goes around the nodes through waypoints. A text area with the code of a shader
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
        lateinit var graphEditor: NodeEditor

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
                            item("Compact nodes") { graphEditor.compactNodes = !graphEditor.compactNodes }
                        }
                    }
                    dock {
                        val graph = panel("Graph") {
                            graphEditor = nodeEditor {
                                val types = mapOf("image" to ColorRGBa.fromHex("#5ab0ff"), "number" to ColorRGBa.fromHex("#f0b44c"))
                                portColor = { types[it.type] }
                                val noise = add(GraphNode("noise", outputs = listOf(Port("image", "image"))), Vector2(40.0, 60.0))
                                val gradient = add(GraphNode("gradient", outputs = listOf(Port("image", "image"))), Vector2(40.0, 200.0))
                                val blur = add(GraphNode("blur", listOf(Port("image", "image"), Port("radius", "number")), listOf(Port("image", "image"))), Vector2(260.0, 60.0))
                                val mix = add(GraphNode("mix", listOf(Port("a", "image"), Port("b", "image"), Port("t", "number")), listOf(Port("image", "image"))), Vector2(480.0, 120.0))
                                val time = add(GraphNode("time", outputs = listOf(Port("seconds", "number"))), Vector2(260.0, 260.0))
                                // the value of the time on its port, and a wave of it in its body
                                // the amount of the mix, a slider in its body while nothing goes into its t
                                var amount = 0.5
                                portLabel = { n, port, _ ->
                                    when {
                                        n === time -> "%.1f".format(seconds)
                                        n === mix && port.name == "t" && wires.none { it.to === mix && it.input == 2 } -> "%.2f".format(amount)
                                        else -> null
                                    }
                                }
                                mix.bodyHeight = 28.0
                                mix.body = { d, box ->
                                    val track = Rectangle(box.x + 10.0, box.center.y - 2.0, box.width - 20.0, 4.0)
                                    d.stroke = null
                                    d.fill = ColorRGBa.WHITE.opacify(0.15)
                                    d.rectangle(track)
                                    d.fill = types["number"]
                                    d.rectangle(track.x, track.y, track.width * amount, track.height)
                                    d.circle(track.x + track.width * amount, track.center.y, 6.0)
                                }
                                mix.bodyPointer = { e ->
                                    amount = ((e.position.x - e.box.x - 10.0) / (e.box.width - 20.0)).coerceIn(0.0, 1.0)
                                    true
                                }
                                time.bodyHeight = 40.0
                                time.body = { d, box ->
                                    d.stroke = types["number"]
                                    d.strokeWeight = 1.5
                                    d.lineStrip((0..60).map { i ->
                                        val x = i / 60.0
                                        Vector2(box.x + 8.0 + x * (box.width - 16.0), box.center.y + sin(x * 12.0 - seconds * 3.0) * box.height * 0.35)
                                    })
                                }
                                val output = add(GraphNode("output", listOf(Port("image", "image"))), Vector2(700.0, 140.0))
                                // the output tells when nothing goes into it
                                fun check() {
                                    val missing = wires.none { it.to === output }
                                    output.status = if (missing) "no input" else null
                                    output.statusColor = if (missing) ColorRGBa.fromHex("#ff7a6e") else null
                                }
                                events.connected.listen { check() }
                                events.disconnected.listen { check() }
                                check()
                                connect(noise, 0, blur, 0)
                                connect(blur, 0, mix, 0)
                                connect(gradient, 0, mix, 1)
                                // the wire from the time back to the radius of the blur goes around both nodes
                                connect(time, 0, blur, 1)
                                wireRoute = { w ->
                                    if (w.from === time && w.to === blur) listOf(Vector2(time.bounds.x + time.width + 30.0, blur.position.y + blur.height + 20.0),
                                        Vector2(blur.position.x - 30.0, blur.position.y + blur.height + 20.0)) else null
                                }
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
        // the live value and the wave of the time are drawn again every frame
        extend { graphEditor.requestRedraw() }
        extend(cm)
    }
}

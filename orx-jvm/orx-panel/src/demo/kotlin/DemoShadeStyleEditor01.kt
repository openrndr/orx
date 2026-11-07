import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ShadeStyle
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * A node based editor of shade styles. The nodes of the graph are pieces of GLSL with typed inputs and outputs, wired
 * into the color of the output node; the graph is compiled to the fragment transform of a shade style, which colors the
 * preview. The parameters of the nodes are uniforms of the shade style: a selected node shows sliders for its
 * parameters, which change the preview while they are dragged without compiling the shade style again; only adding,
 * removing and wiring nodes compiles it. The generated GLSL is shown, highlighted.
 *
 * A right click on the graph adds a node where the pointer is, so does the Add menu in the middle of the graph; inputs
 * of different types are converted, an input without a wire takes its default, delete removes the selected nodes and
 * mod+z undoes the edits.
 */
fun main() = application {
    configure {
        width = 1280
        height = 800
        windowResizable = true
    }
    program {
        val kinds = nodeKinds()
        val graph = ShaderGraph()
        lateinit var editor: NodeEditor
        lateinit var preview: DockPanel
        lateinit var parameters: Div
        lateinit var code: TextArea
        var style: ShadeStyle? = null
        var compiled: String? = null
        var error: String? = null

        fun add(kind: NodeKind, position: Vector2): GraphNode {
            val instance = NodeInstance(kind, graph.nextId++)
            return editor.add(GraphNode(kind.name, kind.inputs.map { Port(it.name, it.type) }, kind.outputs.map { Port(it.name, it.type) }, data = instance), position)
        }

        // the sliders of the parameters of the selected node
        fun showParameters(node: GraphNode?) {
            for (child in parameters.children.toList()) parameters.remove(child)?.close()
            val instance = node?.data as? NodeInstance
            if (instance == null) {
                parameters.p { "Select a node to edit its parameters." }
            } else {
                parameters.h3 { "${instance.kind.name} ${instance.id}" }
                if (instance.kind.parameters.isEmpty()) parameters.p { "This node has no parameters." }
                for (parameter in instance.kind.parameters) {
                    parameters.slider {
                        label = parameter.name
                        range = Range(parameter.range.start, parameter.range.endInclusive)
                        precision = 3
                        value = instance.values.getValue(parameter.name)
                    }.events.valueChanged.listen { instance.values[parameter.name] = it.newValue }
                }
            }
            parameters.requestLayout()
        }

        // the preview shows through its transparent panel
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            val source = graph.compile(editor)
            if (source != compiled) {
                compiled = source
                error = null
                style = shadeStyle {
                    fragmentPreamble = GLSL_HELPERS
                    fragmentTransform = source
                }
                code.value = "// the fragment transform of the shade style\n$source"
            }
            val area = preview.content.screenArea
            val s = style ?: return@extend
            if (area.width <= 0.0 || error != null) return@extend
            // the parameters of the nodes are uniforms, set every frame
            s.parameter("time", seconds)
            for (node in editor.nodes) {
                val instance = node.data as NodeInstance
                for ((name, value) in instance.values) s.parameter("n${instance.id}_$name", value)
            }
            drawer.isolated {
                drawer.shadeStyle = s
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE
                val size = minOf(area.width, area.height) - 24.0
                try {
                    drawer.rectangle(area.center.x - size / 2.0, area.center.y - size / 2.0, size, size)
                } catch (e: Throwable) {
                    error = e.message
                    code.value = "// the shade style does not compile\n// ${e.message?.lines()?.firstOrNull()}\n$source"
                }
            }
        }

        val cm = controlManager {
            css("""
                .page { display: flex; flex-direction: column; align-items: stretch; width: 100%; height: 100% }
                .page > dock { flex: 1; flex-basis: 0 }
                dock-content { background-color: #1d2026 }
                .graph, .code { padding: 0 }
                .graph > node-editor { height: 100% }
                .code > text-area { height: 100%; margin: 0 }
                .preview { background-color: transparent }
                .parameters h3 { margin: 4px 5px 8px 5px }
                .parameters p { margin: 4px 5px; color: #9aa3b2 }
            """)
            layout {
                div("page") {
                    menuBar {
                        menu("Add") {
                            for (kind in kinds.filter { it.name != "output" }) item(kind.name) {
                                val center = editor.graphPosition(Vector2(editor.layout.screenX + editor.layout.screenWidth / 2.0, editor.layout.screenY + editor.layout.screenHeight / 2.0))
                                add(kind, center - Vector2(75.0, 30.0))
                            }
                        }
                    }
                    dock {
                        val graphPanel = panel("Graph") {
                            editor = nodeEditor {
                                // inputs of other types are converted
                                canConnect = { _, _ -> true }
                                selectionChanged(this) { nodes -> showParameters(nodes.singleOrNull()) }
                            }
                        }.also { it.content.classes.add(ElementClass("graph")) }
                        preview = panel("Preview", DockZone.RIGHT, graphPanel) { }.also { it.content.classes.add(ElementClass("preview")) }
                        panel("Parameters", DockZone.BOTTOM, preview) {
                            parameters = div("parameters") { }
                        }
                        panel("GLSL", DockZone.BOTTOM, graphPanel) {
                            code = textArea { highlighter = Highlighters.glsl }
                        }.also { it.content.classes.add(ElementClass("code")) }
                    }
                }
            }
        }
        extend(cm)

        // a right click adds a node where the pointer is
        var pointer = Vector2(200.0, 200.0)
        editor.mouse.moved.listen { pointer = editor.graphPosition(it.position) }
        editor.contextMenu {
            for (kind in kinds.filter { it.name != "output" }) item("Add ${kind.name}") { add(kind, pointer) }
        }

        // the first graph: rings around the center that move with time, between two colors
        fun kind(name: String) = kinds.first { it.name == name }
        val uv = add(kind("uv"), Vector2(20.0, 40.0))
        val distance = add(kind("distance"), Vector2(190.0, 40.0))
        val time = add(kind("time"), Vector2(190.0, 190.0))
        val sum = add(kind("add"), Vector2(360.0, 80.0))
        val rings = add(kind("rings"), Vector2(530.0, 80.0))
        val edge = add(kind("smoothstep"), Vector2(700.0, 80.0))
        val a = add(kind("color"), Vector2(700.0, 230.0))
        val b = add(kind("color"), Vector2(700.0, 340.0))
        val mix = add(kind("mix"), Vector2(870.0, 180.0))
        val output = add(kind("output"), Vector2(1040.0, 180.0))
        (b.data as NodeInstance).values.putAll(mapOf("r" to 0.97, "g" to 0.51, "b" to 0.47))
        (a.data as NodeInstance).values.putAll(mapOf("r" to 0.09, "g" to 0.10, "b" to 0.13))
        editor.connect(uv, 0, distance, 0)
        editor.connect(distance, 0, sum, 0)
        editor.connect(time, 0, sum, 1)
        editor.connect(sum, 0, rings, 0)
        editor.connect(rings, 0, edge, 0)
        editor.connect(a, 0, mix, 0)
        editor.connect(b, 0, mix, 1)
        editor.connect(edge, 0, mix, 2)
        editor.connect(mix, 0, output, 0)
        editor.pan = Vector2(10.0, 20.0)
        editor.zoom = 0.52
        showParameters(null)
    }
}

/** calls [f] with the selected nodes when they change */
private fun selectionChanged(editor: NodeEditor, f: (Set<GraphNode>) -> Unit) {
    editor.events.selected.listen(f)
}

//<editor-fold desc="the nodes">
/** an input of a kind of node: its GLSL [type] and the expression it takes without a wire */
private class Input(val name: String, val type: String, val default: String)

private class Output(val name: String, val type: String)

/** a parameter of a kind of node: a uniform in [range] */
private class Parameter(val name: String, val default: Double, val range: ClosedRange<Double>)

/**
 * A kind of node: [code] gives the GLSL expressions of the outputs, from the expressions of the inputs and the names of
 * the uniforms of the parameters; the output node has no outputs and gives the statement that sets the fill
 */
private class NodeKind(
    val name: String,
    val inputs: List<Input>,
    val outputs: List<Output>,
    val parameters: List<Parameter> = emptyList(),
    val code: (inputs: List<String>, parameter: (String) -> String) -> List<String>
)

/** a node of the graph: its kind, its number, which names its uniforms, and the values of its parameters */
private class NodeInstance(val kind: NodeKind, val id: Int) {
    val values = kind.parameters.associate { it.name to it.default }.toMutableMap()
}

private fun nodeKinds(): List<NodeKind> {
    val uv = "c_boundsPosition.xy"
    return listOf(
        NodeKind("uv", emptyList(), listOf(Output("uv", "vec2"))) { _, _ -> listOf(uv) },
        NodeKind("time", emptyList(), listOf(Output("seconds", "float")), listOf(Parameter("speed", 0.3, 0.0..3.0))) { _, p ->
            listOf("p_time * ${p("speed")}")
        },
        NodeKind("number", emptyList(), listOf(Output("value", "float")), listOf(Parameter("value", 1.0, 0.0..10.0))) { _, p ->
            listOf(p("value"))
        },
        NodeKind("color", emptyList(), listOf(Output("color", "vec3")),
            listOf(Parameter("r", 0.35, 0.0..1.0), Parameter("g", 0.69, 0.0..1.0), Parameter("b", 1.0, 0.0..1.0))) { _, p ->
            listOf("vec3(${p("r")}, ${p("g")}, ${p("b")})")
        },
        NodeKind("distance", listOf(Input("uv", "vec2", uv)), listOf(Output("distance", "float")),
            listOf(Parameter("x", 0.5, 0.0..1.0), Parameter("y", 0.5, 0.0..1.0))) { i, p ->
            listOf("length(${i[0]} - vec2(${p("x")}, ${p("y")}))")
        },
        NodeKind("sine", listOf(Input("x", "float", "0.0")), listOf(Output("wave", "float")),
            listOf(Parameter("frequency", 10.0, 0.0..60.0), Parameter("phase", 0.0, -3.1416..3.1416))) { i, p ->
            listOf("0.5 + 0.5 * sin(${i[0]} * ${p("frequency")} + ${p("phase")})")
        },
        NodeKind("noise", listOf(Input("position", "vec2", uv)), listOf(Output("noise", "float")),
            listOf(Parameter("scale", 6.0, 0.5..40.0), Parameter("octaves", 4.0, 1.0..6.0))) { i, p ->
            listOf("fbm(${i[0]} * ${p("scale")}, int(${p("octaves")}))")
        },
        NodeKind("rings", listOf(Input("distance", "float", "0.0")), listOf(Output("rings", "float")),
            listOf(Parameter("count", 8.0, 1.0..40.0))) { i, p ->
            listOf("fract(${i[0]} * ${p("count")})")
        },
        NodeKind("smoothstep", listOf(Input("x", "float", "0.0")), listOf(Output("step", "float")),
            listOf(Parameter("from", 0.4, 0.0..1.0), Parameter("to", 0.6, 0.0..1.0))) { i, p ->
            listOf("smoothstep(${p("from")}, ${p("to")}, ${i[0]})")
        },
        NodeKind("add", listOf(Input("a", "float", "0.0"), Input("b", "float", "0.0")), listOf(Output("sum", "float"))) { i, _ ->
            listOf("(${i[0]} + ${i[1]})")
        },
        NodeKind("multiply", listOf(Input("a", "float", "1.0"), Input("b", "float", "1.0")), listOf(Output("product", "float")),
            listOf(Parameter("gain", 1.0, 0.0..4.0))) { i, p ->
            listOf("(${i[0]} * ${i[1]} * ${p("gain")})")
        },
        NodeKind("mix", listOf(Input("a", "vec3", "vec3(0.0)"), Input("b", "vec3", "vec3(1.0)"), Input("t", "float", "0.5")),
            listOf(Output("color", "vec3"))) { i, _ ->
            listOf("mix(${i[0]}, ${i[1]}, clamp(${i[2]}, 0.0, 1.0))")
        },
        NodeKind("hue", listOf(Input("hue", "float", "0.0")), listOf(Output("color", "vec3")),
            listOf(Parameter("saturation", 0.7, 0.0..1.0), Parameter("value", 0.9, 0.0..1.0))) { i, p ->
            listOf("hsv2rgb(vec3(${i[0]}, ${p("saturation")}, ${p("value")}))")
        },
        NodeKind("output", listOf(Input("color", "vec3", "vec3(0.5)")), emptyList()) { i, _ ->
            // the colors of the graph are in sRGB, those of the fill linear
            listOf("x_fill = vec4(srgbToLinear(clamp(${i[0]}, 0.0, 1.0)), 1.0);")
        }
    )
}

/** functions that the nodes use */
private const val GLSL_HELPERS = """
float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}
float fbm(vec2 p, int octaves) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 6; i++) {
        if (i >= octaves) break;
        sum += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return sum;
}
vec3 srgbToLinear(vec3 c) { return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(0.04045, c)); }
vec3 hsv2rgb(vec3 c) {
    vec3 p = abs(fract(c.xxx + vec3(1.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
    return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), c.y);
}
"""
//</editor-fold>

/** compiles the graph of a [NodeEditor] to the fragment transform of a shade style */
private class ShaderGraph {
    var nextId = 1

    /** [expression] of [from] as [to] */
    private fun convert(expression: String, from: String, to: String): String = when {
        from == to -> expression
        from == "float" -> "$to($expression)"
        to == "float" -> "($expression).x"
        from == "vec2" && to == "vec3" -> "vec3($expression, 0.0)"
        from == "vec3" && to == "vec2" -> "($expression).xy"
        else -> "$to($expression)"
    }

    fun compile(editor: NodeEditor): String {
        val lines = mutableListOf<String>()
        val done = mutableSetOf<GraphNode>()
        val visiting = mutableSetOf<GraphNode>()

        /** the variable of the output [index] of [node], declared once its inputs are */
        fun variable(node: GraphNode, index: Int) = "n${(node.data as NodeInstance).id}_${index}"

        fun emit(node: GraphNode): Boolean {
            if (node in done) return true
            // a wire that closes a loop is left out, its input takes its default
            if (!visiting.add(node)) return false
            val instance = node.data as NodeInstance
            val inputs = instance.kind.inputs.mapIndexed { i, input ->
                val wire = editor.wires.firstOrNull { it.to === node && it.input == i }
                if (wire != null && emit(wire.from)) {
                    val type = (wire.from.data as NodeInstance).kind.outputs[wire.output].type
                    convert(variable(wire.from, wire.output), type, input.type)
                } else input.default
            }
            val expressions = instance.kind.code(inputs) { name -> "p_n${instance.id}_$name" }
            if (instance.kind.outputs.isEmpty()) {
                lines.add(expressions.single())
            } else {
                for ((k, output) in instance.kind.outputs.withIndex()) {
                    lines.add("${output.type} ${variable(node, k)} = ${expressions[k]}; // ${instance.kind.name} ${instance.id}")
                }
            }
            visiting.remove(node)
            done.add(node)
            return true
        }

        val output = editor.nodes.firstOrNull { (it.data as NodeInstance).kind.name == "output" }
        if (output == null) return "x_fill = vec4(vec3(0.5), 1.0);"
        emit(output)
        return lines.joinToString("\n")
    }
}

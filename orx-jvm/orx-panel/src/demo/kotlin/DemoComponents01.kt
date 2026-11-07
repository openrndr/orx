import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * Demonstrates the components of panels: a split pane with a tree on the left and a table on the right, rich text with
 * spans in their own styles and a link, a dialog that keeps the keyboard focus, and toasts.
 */
fun main() = application {
    configure {
        width = 960
        height = 600
        windowResizable = true
    }
    program {
        class Node(val name: String, val children: List<Node> = emptyList())
        val tree = Node("scene", List(12) { i -> Node("group $i", List(i % 4 + 1) { j -> Node("shape $i.$j") }) })
        data class Shape(val name: String, val kind: String, val area: Double)
        val kinds = listOf("circle", "rectangle", "triangle", "contour")
        val shapes = List(5000) { Shape("shape $it", kinds[it % 4], (it * 7919 % 1000) / 10.0) }

        val cm = controlManager {
            css("""
                .page { display: flex; flex-direction: column; align-items: stretch; width: 100%; height: 100%; padding: 12px; row-gap: 12px }
                rich-text { width: 100%; font-size: 15px; color: #c9d1e0 }
                span.strong { font-weight: 700; color: white }
                span.code { color: #7fd1b9; letter-spacing: 1px }
                span.link { color: #5ab0ff; text-decoration: underline }
                .bar { display: flex; flex-direction: row; column-gap: 8px }
                .content { flex: 1; flex-basis: 0 }
                .split-first, .split-second { background-color: #20242c }
                .dialog p { width: 260px; margin-bottom: 8px }
            """)
            layout {
                div("page") {
                    richText {
                        span { "Panels have " }
                        span("strong") { "trees, tables, split panes, dialogs and toasts" }
                        span { ", built on " }
                        span("code") { "VirtualList" }
                        span { " so that long lists stay quick. Text runs in its own styles in one paragraph, " }
                        span("link") { "like this link" }.mouse.clicked.listen { this@program.extensions.filterIsInstance<org.openrndr.panel.ControlManager>().firstOrNull()?.toast("the link was clicked") }
                        span { "." }
                    }
                    div("bar") {
                        button { label = "Open dialog" }.events.clicked.listen {
                            val cm = this@program.extensions.filterIsInstance<org.openrndr.panel.ControlManager>().first()
                            lateinit var dialog: Dialog
                            dialog = cm.dialog {
                                h3 { "A dialog" }
                                p { "The keyboard focus stays in it, escape or a click outside closes it." }
                                button { label = "OK" }.events.clicked.listen { dialog.close() }
                                button { label = "Cancel" }.events.clicked.listen { dialog.close() }
                            }
                        }
                        button { label = "Toast" }.events.clicked.listen {
                            this@program.extensions.filterIsInstance<org.openrndr.panel.ControlManager>().first().toast("Saved ${shapes.size} shapes")
                        }
                    }
                    div("content") {
                        splitPane {
                            position = 260.0
                            first {
                                treeView(listOf(tree), { it.children }, { it.name }) { expanded.add(tree) }
                            }
                            second {
                                dataTable(shapes, listOf(
                                    Column("name", 160.0, { it.name }),
                                    Column("kind", 120.0, { it.kind }),
                                    Column("area", 100.0, { it.area }, { "%.1f".format(it as Double) })
                                ))
                            }
                        }
                    }
                }
            }
        }
        extend(cm)
        extend { drawer.clear(ColorRGBa.fromHex("#16181c")) }
    }
}

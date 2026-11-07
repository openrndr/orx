import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.panel.controlManager
import org.openrndr.panel.elements.*

/**
 * Demonstrates the tools of panels: a menu bar whose items are also commands in the command palette (cmd/ctrl+K),
 * panels in a dock that are dragged by their tabs into other groups and out of the window, a knob, a range slider, a
 * logarithmic slider with ticks, number fields that take arithmetic, a curve editor, a gradient editor, a list that is
 * reordered by dragging and a timeline of keyframes that animates the drawing.
 */
fun main() = application {
    configure {
        width = 1100
        height = 720
        windowResizable = true
    }
    program {
        var x = 0.5
        var radius = 60.0
        var scale = 1.0
        var curve = CubicBezier.EASE
        lateinit var gradient: GradientEditor
        lateinit var timeline: Timeline
        lateinit var viewport: DockPanel
        lateinit var dock: Dock
        val layers = listOf("background", "circles", "outlines", "labels", "grid")

        // the drawing shows through the viewport panel, so it is drawn before the panels
        fun org.openrndr.draw.Drawer.scene(area: org.openrndr.shape.Rectangle) {
            if (area.width <= 0.0) return
            drawStyle.clip = area
            val circles = 8
            for (i in 0 until circles) {
                val t = i / (circles - 1.0)
                fill = gradient.colorAt(t)
                stroke = null
                circle(area.x + area.width * (0.15 + 0.7 * x), area.y + area.height * (0.15 + 0.7 * curve.ease(t)), radius * scale * (0.4 + 0.6 * t) * area.height / 600.0)
            }
            drawStyle.clip = null
        }
        extend {
            drawer.clear(ColorRGBa.fromHex("#16181c"))
            // in a window of its own, the viewport is drawn there
            if (viewport.window == null) drawer.scene(viewport.content.screenArea)
        }

        val cm = controlManager {
            css("""
                .page { display: flex; flex-direction: column; align-items: stretch; width: 100%; height: 100% }
                .page > dock { flex: 1; flex-basis: 0 }
                .knobs { display: flex; flex-direction: row }
                dock-content { background-color: #1d2026 }
                .viewport { background-color: transparent }
                .layer { width: 100%; height: 100%; padding-left: 8px; font-size: 13px; color: #c9d1e0 }
            """)
            layout {
                div("page") {
                    menuBar {
                        menu("File") {
                            item("New", "mod+n") { timeline.time = 0.0 }
                            item("Save", "mod+s") { (root() as Body).controlManager?.toast("Saved") }
                            separator()
                            submenu("Export") {
                                item("PNG") { (root() as Body).controlManager?.toast("Exported a PNG") }
                                item("SVG") { (root() as Body).controlManager?.toast("Exported an SVG") }
                            }
                        }
                        menu("Playback") {
                            item("Play or pause", "mod+p") { timeline.playing = !timeline.playing }
                            item("To the start") { timeline.time = 0.0 }
                        }
                        menu("Window") {
                            item("Dock all panels") { dock.floating.toList().forEach { dock.redock(it) } }
                        }
                    }
                    dock = dock {
                        val view = panel("Viewport") { }.also {
                            it.content.classes.add(ElementClass("viewport"))
                            it.inWindow = { extend { drawer.scene(viewport.content.screenArea) } }
                            viewport = it
                        }
                        val controls = panel("Controls", DockZone.LEFT, view) {
                            div("knobs") {
                                knob { label = "radius"; range = 10.0..120.0; default = 60.0; value = radius }
                                    .valueChanged.listen { radius = it.newValue }
                                knob { label = "x"; range = 0.0..1.0; default = 0.5; value = x }
                                    .valueChanged.listen { x = it.newValue }
                            }
                            slider {
                                label = "scale"; range = Range(0.1, 10.0); logarithmic = true; ticks = 2; value = scale
                            }.events.valueChanged.listen { scale = it.newValue }
                            rangeSlider { label = "range"; range = 0.0..100.0; value = 20.0..80.0; precision = 0 }
                            numberField { label = "radius (type 2*30, 50%)"; range = 10.0..120.0; value = radius }
                                .events.valueChanged.listen { radius = it.newValue }
                        }
                        panel("Layers", DockZone.CENTER, controls) {
                            virtualList(layers, 28.0) { item, _ -> div("layer") { p { item } } }.apply {
                                reorderable = true
                                reordered.listen { (from, to) -> items = items.toMutableList().apply { add(to, removeAt(from)) } }
                            }
                        }
                        panel("Editors", DockZone.RIGHT, view) {
                            curveEditor { label = "positions" }.valueChanged.listen { curve = it.newValue }
                            gradient = gradientEditor {
                                value = listOf(ColorStop(0.0, ColorRGBa.fromHex("#5ab0ff")), ColorStop(0.5, ColorRGBa.fromHex("#f88379")), ColorStop(1.0, ColorRGBa.fromHex("#ffe08a")))
                            }
                        }
                        panel("Timeline", DockZone.BOTTOM, view) {
                            timeline = timeline {
                                duration = 6.0
                                track("x", 0.0..1.0, org.openrndr.extra.style.TimingFunction.EASE_IN_OUT, { x }) { x = it }.keyframes =
                                    listOf(Keyframe(0.0, 0.1), Keyframe(3.0, 0.9), Keyframe(6.0, 0.1))
                                track("radius", 10.0..120.0, current = { radius }) { radius = it }.keyframes = listOf(Keyframe(0.0, 30.0), Keyframe(1.5, 110.0), Keyframe(4.5, 40.0))
                                track("scale", 0.1..2.0, current = { scale }) { scale = it }
                                time = 1.0
                            }
                        }
                        select(controls)
                    }
                }
            }
        }
        extend(cm)
    }
}

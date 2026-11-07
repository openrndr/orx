package org.openrndr.panel.elements

import org.openrndr.MouseButton
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.draw.shadeStyle
import org.openrndr.events.Event
import org.openrndr.extra.color.spaces.ColorOKLCHa
import org.openrndr.extra.color.spaces.toOKLABa
import org.openrndr.extra.style.parseCssColor
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.panel.binding.PropertyBinding
import org.openrndr.panel.binding.bindValue
import org.openrndr.panel.recordEdit
import org.openrndr.panel.style.effectiveColor
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.reflect.KMutableProperty0

/** the largest chroma of the wheel of a [ColorEditor], about the largest in sRGB */
private const val MAX_CHROMA = 0.32

/**
 * A color in OKLCH, the hue kept when the chroma is 0 so that grays keep the hue they were given
 */
internal data class Lch(val l: Double, val c: Double, val h: Double, val alpha: Double) {
    fun toRGBa(): ColorRGBa = ColorOKLCHa(l, c, h, alpha).toRGBa().toSRGB().let {
        ColorRGBa(it.r.coerceIn(0.0, 1.0), it.g.coerceIn(0.0, 1.0), it.b.coerceIn(0.0, 1.0), alpha.coerceIn(0.0, 1.0), Linearity.SRGB)
    }

    companion object {
        fun of(color: ColorRGBa, hue: Double): Lch {
            val lab = color.toOKLABa()
            val c = sqrt(lab.a * lab.a + lab.b * lab.b)
            val h = if (c < 1E-4) hue else Math.toDegrees(atan2(lab.b, lab.a)).let { if (it < 0.0) it + 360.0 else it }
            return Lch(lab.l, c, h, color.alpha)
        }
    }
}

/**
 * Edits a color: a wheel of the hues and chromas of OKLCH at the lightness of the color, sliders of its lightness,
 * chroma, hue and alpha, a field that takes any CSS color, like `#ff8800` or `oklch(70% 0.1 200)`, swatches of saved
 * colors that all color editors share, and a button that picks a color from the window. The changes by the user can
 * be undone.
 */
class ColorEditor : Div(ElementType("color-editor")) {
    companion object {
        /** the saved colors, shared by the color editors */
        val swatches = mutableListOf<ColorRGBa>()

        /** the saved colors changed */
        val swatchesChanged = Event<List<ColorRGBa>>("color-editor-swatches-changed")

        fun addSwatch(color: ColorRGBa) {
            if (color in swatches) return
            swatches.add(color)
            swatchesChanged.trigger(swatches.toList())
        }

        fun removeSwatch(color: ColorRGBa) {
            if (swatches.remove(color)) swatchesChanged.trigger(swatches.toList())
        }
    }

    val valueChanged = Event<ValueChange<ColorRGBa>>("color-editor-value-changed")

    private var lch = Lch(0.7, 0.12, 250.0, 1.0)

    /** the color */
    var value: ColorRGBa
        get() = lch.toRGBa()
        set(v) = set(Lch.of(v.toSRGB(), lch.h), false)

    private fun set(new: Lch, interactive: Boolean) {
        if (new == lch) return
        val old = value
        lch = new
        update()
        val now = value
        if (now != old) {
            valueChanged.trigger(ValueChange(old, now, interactive))
            if (interactive) recordEdit("Change color", old, now) { value = it }
        }
    }

    private val wheel = ColorWheel(this)
    private val lightness = Slider().apply { label = "L"; range = Range(0.0, 1.0); precision = 3 }
    private val chroma = Slider().apply { label = "C"; range = Range(0.0, MAX_CHROMA); precision = 3 }
    private val hue = Slider().apply { label = "H"; range = Range(0.0, 360.0); precision = 1 }
    private val alpha = Slider().apply { label = "alpha"; range = Range(0.0, 1.0); precision = 2 }
    private val input = TextInput().apply { classes.add(ElementClass("color-css")) }
    private val swatchRow = Div().apply { classes.add(ElementClass("color-swatches")) }
    private var updating = false

    init {
        append(wheel)
        append(lightness)
        append(chroma)
        append(hue)
        append(alpha)
        append(input)
        append(swatchRow)
        for ((slider, change) in listOf<Pair<Slider, (Lch, Double) -> Lch>>(
            lightness to { c, v -> c.copy(l = v) }, chroma to { c, v -> c.copy(c = v) },
            hue to { c, v -> c.copy(h = v) }, alpha to { c, v -> c.copy(alpha = v) }
        )) {
            slider.events.valueChanged.listen { if (it.interactive && !updating) set(change(lch, it.newValue), true) }
        }
        // a CSS color that is typed is taken as soon as it is one
        input.events.valueChanged.listen {
            if (updating) return@listen
            parseCssColor(it.newValue.trim())?.let { c -> set(Lch.of(c.toSRGB(), lch.h), true) }
        }
        swatchesChanged.listen { buildSwatches() }
        buildSwatches()
        update()
    }

    private fun update() {
        updating = true
        lightness.value = lch.l
        chroma.value = lch.c
        hue.value = lch.h
        alpha.value = lch.alpha
        // the field shows the color, but not while it is typed in
        if ((root() as? Body)?.controlManager?.keyboardInput?.target !== input) input.value = cssText(value)
        updating = false
        wheel.requestRedraw()
        swatchRow.children.forEach { it.requestRedraw() }
    }

    internal fun pickWheel(position: Vector2, interactive: Boolean) {
        val center = Vector2(wheel.layout.screenX + wheel.layout.screenWidth / 2.0, wheel.layout.screenY + wheel.layout.screenHeight / 2.0)
        val d = position - center
        val r = d.length / wheel.radius
        val h = Math.toDegrees(atan2(d.y, d.x)).let { if (it < 0.0) it + 360.0 else it }
        set(lch.copy(c = r.coerceIn(0.0, 1.0) * MAX_CHROMA, h = h), interactive)
    }

    internal val wheelLch get() = lch

    private fun buildSwatches() {
        for (child in swatchRow.children.toList()) swatchRow.remove(child)?.close()
        for (color in swatches) swatchRow.append(ColorSwatch(color).apply {
            mouse.clicked.listen {
                if (it.button == MouseButton.LEFT) set(Lch.of(color, lch.h), true)
                it.cancelPropagation()
            }
            contextMenu { item("Remove swatch") { removeSwatch(color) } }
        })
        swatchRow.append(Button().apply {
            label = "+"
            classes.add(ElementClass("color-add"))
            events.clicked.listen { addSwatch(value) }
        })
        swatchRow.append(Button().apply {
            label = "Pick"
            classes.add(ElementClass("color-pick"))
            events.clicked.listen {
                (root() as? Body)?.controlManager?.pickColor { c -> set(Lch.of(c, lch.h), true) }
            }
        })
        swatchRow.requestLayout()
    }

    override fun close() {
        super.close()
        valueChanged.close()
    }
}

/** [c] as CSS: `#rrggbb` when it is opaque, `rgb(r g b / a)` otherwise */
internal fun cssText(c: ColorRGBa): String {
    fun byte(v: Double) = (v.coerceIn(0.0, 1.0) * 255.0 + 0.5).toInt()
    return if (c.alpha >= 0.999) "#%02x%02x%02x".format(byte(c.r), byte(c.g), byte(c.b))
    else "rgb(${byte(c.r)} ${byte(c.g)} ${byte(c.b)} / ${"%.2f".format(c.alpha)})"
}

/** a saved color of a [ColorEditor], a click takes it */
class ColorSwatch internal constructor(val color: ColorRGBa) : Element(ElementType("color-swatch")) {
    init {
        mouse.pressed.listen { it.cancelPropagation() }
    }

    override fun draw(drawer: Drawer) {
        drawer.stroke = (computedStyle.effectiveColor ?: ColorRGBa.WHITE).opacify(0.3)
        drawer.strokeWeight = 1.0
        drawer.fill = color
        drawer.rectangle(0.0, 0.0, layout.screenWidth, layout.screenHeight)
    }
}

/** the wheel of a [ColorEditor]: the hues around, the chromas outward, at the lightness of the color */
class ColorWheel internal constructor(private val editor: ColorEditor) : Element(ElementType("color-wheel")) {
    internal val radius get() = minOf(layout.screenWidth, layout.screenHeight) / 2.0 - 4.0

    init {
        mouse.pressed.listen {
            editor.pickWheel(it.position, true)
            it.cancelPropagation()
        }
        mouse.dragged.listen {
            editor.pickWheel(it.position, true)
            it.cancelPropagation()
        }
    }

    override fun draw(drawer: Drawer) {
        val lch = editor.wheelLch
        val center = Vector2(layout.screenWidth / 2.0, layout.screenHeight / 2.0)
        drawer.isolated {
            drawer.stroke = null
            drawer.fill = ColorRGBa.WHITE
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    vec2 p = c_boundsPosition.xy * 2.0 - 1.0;
                    float r = length(p);
                    float h = atan(p.y, p.x);
                    float C = min(r, 1.0) * p_maxChroma;
                    vec3 lab = vec3(p_l, C * cos(h), C * sin(h));
                    float l_ = lab.x + 0.3963377774 * lab.y + 0.2158037573 * lab.z;
                    float m_ = lab.x - 0.1055613458 * lab.y - 0.0638541728 * lab.z;
                    float s_ = lab.x - 0.0894841775 * lab.y - 1.2914855480 * lab.z;
                    float l = l_ * l_ * l_;
                    float m = m_ * m_ * m_;
                    float s = s_ * s_ * s_;
                    vec3 lin = vec3(
                        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s);
                    bool outside = any(lessThan(lin, vec3(-0.002))) || any(greaterThan(lin, vec3(1.002)));
                    // the fill is linear
                    vec3 c = clamp(lin, 0.0, 1.0);
                    // the colors outside sRGB are dimmed
                    if (outside) c = mix(c, vec3(dot(c, vec3(0.2126, 0.7152, 0.0722))), 0.8) * 0.25;
                    x_fill = vec4(c, x_fill.a);
                """
                parameter("maxChroma", MAX_CHROMA)
                parameter("l", lch.l)
            }
            drawer.circle(center, radius)
        }
        // the color, where it is on the wheel
        val p = center + Polar(lch.h, lch.c / MAX_CHROMA * radius).cartesian
        drawer.fill = editor.value.opacify(1.0 / maxOf(editor.value.alpha, 1E-6))
        drawer.stroke = ColorRGBa.WHITE
        drawer.strokeWeight = 2.0
        drawer.circle(p, 6.0)
        drawer.stroke = ColorRGBa.BLACK.opacify(0.5)
        drawer.strokeWeight = 1.0
        drawer.circle(p, 8.0)
    }
}

/** binds the color of the editor to [property], both ways, see [bindValue] */
fun ColorEditor.bind(property: KMutableProperty0<ColorRGBa>): PropertyBinding<ColorRGBa> =
    bindValue(valueChanged, property, { value }, { value = it })

fun Element.colorEditor(vararg classes: String, init: ColorEditor.() -> Unit): ColorEditor = initElement(classes, ColorEditor(), init)

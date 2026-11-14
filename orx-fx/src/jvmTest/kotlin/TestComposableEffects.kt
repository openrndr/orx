import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.ApproximateGaussianBlur
import org.openrndr.extra.fx.blur.HashBlur
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.fx.fx_approximate_gaussian_blur
import org.openrndr.extra.fx.mppFilterShader
import org.openrndr.extra.shadestyles.presets.*
import org.openrndr.extra.testing.AbstractApplicationTestFixture
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestComposableEffects : AbstractApplicationTestFixture() {
    private fun image(width: Int = 64, height: Int = 48, color: (Int, Int) -> ColorRGBa): ColorBuffer =
        colorBuffer(width, height, type = ColorType.FLOAT32).apply {
            for (y in 0 until height) for (x in 0 until width) shadow[x, y] = color(x, y)
            shadow.upload()
        }

    private fun ColorBuffer.pixel(x: Int, y: Int): ColorRGBa {
        shadow.download()
        return shadow[x, y]
    }

    private fun assertColor(expected: ColorRGBa, actual: ColorRGBa, tolerance: Double = 1E-4, message: String = "") {
        assertTrue(
            abs(expected.r - actual.r) < tolerance && abs(expected.g - actual.g) < tolerance && abs(expected.b - actual.b) < tolerance,
            "$message: $actual, not $expected"
        )
    }

    private fun target(source: ColorBuffer) = colorBuffer(source.width, source.height, type = ColorType.FLOAT32)

    @Test
    fun `blend stages blend with the input, an image and a source, also after a pass of their own`() {
        val input = image { _, _ -> ColorRGBa(0.2, 0.4, 0.6) }
        val target = target(input)
        val half = LayerBlend().apply { opacity = 0.5 }
        // the inverted image, halfway to the input
        effectChain { color(Invert()); blend(half, BlendLayer.Input) }.apply(input, target)
        assertColor(ColorRGBa(0.5, 0.5, 0.5), target.pixel(10, 10), message = "with the input")
        // a blur in between starts a pass of its own, the later pass still reads the input of the chain
        val chain = effectChain { color(Invert()); filter(ApproximateGaussianBlur()); blend(half, BlendLayer.Input) }
        assertEquals(3, chain.passCount)
        chain.apply(input, target)
        assertColor(ColorRGBa(0.5, 0.5, 0.5), target.pixel(30, 20), message = "after a blur")
        // an image and a source as the layer
        val red = image { _, _ -> ColorRGBa.RED }
        effectChain { blend(LayerBlend(), BlendLayer.Image(red)) }.apply(input, target)
        assertColor(ColorRGBa.RED, target.pixel(5, 5), message = "with an image")
        effectChain { blend(half, BlendLayer.Source(SolidSource(ColorRGBa.BLUE))) }.apply(input, target)
        assertColor(ColorRGBa(0.1, 0.2, 0.8), target.pixel(5, 5), message = "with a source")
        // a multiplying blend fuses with the stages around it into one pass
        assertEquals(1, effectChain { warp(Ripple()); blend(LayerBlend(LayerBlendMode.MULTIPLY), BlendLayer.Input); color(Grade()) }.passCount)
    }

    /** a color preset that writes the time, the frame and the width of the image into the color */
    private class ContextColor : ColorPreset() {
        override val dynamicParameter = glslFunction<Vector4>(
            "vec4 c, vec2 uv", "return vec4(fract(fx_time()), float(fx_frame()) / 10.0, fx_resolution().x / 100.0, 1.0);",
            listOf(fxContextLibrary)
        )
    }

    @Test
    fun `presets read the time, the frame and the size of the image`() {
        val input = image { _, _ -> ColorRGBa.BLACK }
        val target = target(input)
        val chain = effectChain { color(ContextColor()) }
        chain.time = 2.25
        chain.frame = 3
        chain.apply(input, target)
        assertColor(ColorRGBa(0.25, 0.3, 0.64), target.pixel(0, 0), message = "in a chain")
        // a filter made by something else than a chain sets them itself
        val style = shadeStyle {
            fragmentTransform = "x_fill = p_color(texture(p_input0, c_textureCoordinate), c_textureCoordinate);\n$FX_CONTEXT_USE"
        }
        val filter = ShadeStyleFilter1to1(style)
        PresetSlot(filter, "color", ColorPreset.CONTRACT, ContextColor())
        filter.setFxContext(0.5, 7, Vector2(50.0, 10.0))
        filter.apply(input, target)
        assertColor(ColorRGBa(0.5, 0.7, 0.5), target.pixel(3, 3), message = "set by hand")
    }

    @Test
    fun `the blur with a sigma scale blurs as it did, and as much as the scale says where it says`() {
        // vertical stripes
        val stripes = image { x, _ -> if ((x / 4) % 2 == 0) ColorRGBa.WHITE else ColorRGBa.BLACK }
        val blur = ApproximateGaussianBlur().apply { window = 6; sigma = 2.5; gain = 1.0 }
        val result = target(stripes)
        blur.apply(stripes, result)

        // the shader of before, in two passes
        val before = Filter1to1(mppFilterShader(fx_approximate_gaussian_blur, "approximate gaussian blur"))
        before.parameters["window"] = 6
        before.parameters["sigma"] = 2.5
        before.parameters["spread"] = 1.0
        before.parameters["gain"] = 1.0
        before.parameters["wrapU"] = 0
        before.parameters["wrapV"] = 0
        before.parameters["sourceLevel"] = 0
        val between = target(stripes)
        val expected = target(stripes)
        before.parameters["blurDirection"] = Vector2(1.0, 0.0)
        before.apply(stripes, between)
        before.parameters["blurDirection"] = Vector2(0.0, 1.0)
        before.apply(between, expected)
        for (x in 0 until 64 step 3) assertColor(expected.pixel(x, 20), result.pixel(x, 20), 1E-5, "column $x")

        // no blur where the scale is 0
        blur.sigmaScale = ConstantScalar(0.0)
        blur.apply(stripes, result)
        for (x in 0 until 64 step 5) assertColor(stripes.pixel(x, 10), result.pixel(x, 10), message = "column $x")

        // a scale from 0 at the left to 1 at the right: sharp at the left, blurred at the right
        blur.sigmaScale = LinearGradientScalar().apply { start = Vector2(0.0, 0.5); end = Vector2(1.0, 0.5) }
        blur.apply(stripes, result)
        fun contrast(x0: Int) = (x0 until x0 + 8).map { result.pixel(it, 24).r }.let { it.max() - it.min() }
        assertTrue(contrast(0) > 0.95, "sharp at the left: ${contrast(0)}")
        assertTrue(contrast(54) < 0.6, "blurred at the right: ${contrast(54)}")

        // the slots of the filters, which a graph shows as ports
        assertEquals(listOf("sigmaScale"), blur.presetSlots.map { it.name })
        assertEquals(listOf("radiusScale", "direction"), HashBlur().presetSlots.map { it.name })
        assertEquals(listOf("warp"), Perturb().presetSlots.map { it.name })
    }

    @Test
    fun `film grain changes with the time of the context`() {
        val input = image { _, _ -> ColorRGBa(0.5, 0.5, 0.5) }
        val target = target(input)
        val grain = FilmGrain()
        val chain = effectChain { color(grain) }
        fun frame(time: Double): List<Double> {
            chain.time = time
            chain.apply(input, target)
            return (0 until 16).map { target.pixel(it * 3, 7).r }
        }
        val first = frame(0.0)
        // the same at the same time, another grain a change later
        assertEquals(first, frame(0.0))
        val later = frame(1.0 / grain.rate)
        assertTrue(first.zip(later).count { (a, b) -> abs(a - b) > 1E-4 } > 8, "the grain does not change: $first, $later")
        // the time of the preset is an offset of that of the context
        grain.time = 1.0 / grain.rate
        assertEquals(later, frame(0.0))
        assertTrue(fxContextLibrary in grain.dynamicParameter.libraries)
    }

    @Test
    fun `filters and chains keep images that are transparent in part as they are`() {
        // a disc with a soft edge, its colors premultiplied as images hold them
        fun disc(x: Int, y: Int): ColorRGBa {
            val d = Vector2(x - 32.0, y - 24.0).length
            val a = (1.0 - (d - 10.0) / 10.0).coerceIn(0.0, 1.0) * 0.5 + 0.25
            return ColorRGBa(0.8 * a, 0.4 * a, 0.2 * a, a)
        }
        val input = image(color = ::disc)
        val target = target(input)
        fun assertSame(what: String, tolerance: Double = 1E-4) {
            for (y in 0 until 48 step 3) for (x in 0 until 64 step 3) {
                val e = input.pixel(x, y)
                val c = target.pixel(x, y)
                assertTrue(abs(e.r - c.r) < tolerance && abs(e.g - c.g) < tolerance && abs(e.b - c.b) < tolerance && abs(e.alpha - c.alpha) < tolerance,
                    "$what at $x, $y: $c, not $e")
            }
        }
        effectChain { color(Grade()) }.apply(input, target)
        assertSame("a grade that changes nothing")
        effectChain { color(Grade()); filter(ApproximateGaussianBlur().apply { sigmaScale = ConstantScalar(0.0) }); blend(LayerBlend(), BlendLayer.Input) }.apply(input, target)
        assertSame("a chain that blends with its input after a pass of its own")
        ApproximateGaussianBlur().apply { sigmaScale = ConstantScalar(0.0) }.apply(input, target)
        assertSame("a blur of sigma 0")
        Perturb().apply { warp = Ripple().apply { amount = 0.0 } }.apply(input, target)
        assertSame("a perturb that moves nothing")

        // the blur blurs as the shader of before did, also where the image is transparent in part
        val blur = ApproximateGaussianBlur().apply { window = 6; sigma = 2.5 }
        blur.apply(input, target)
        val before = Filter1to1(mppFilterShader(fx_approximate_gaussian_blur, "approximate gaussian blur"))
        for ((k, v) in listOf("window" to 6, "wrapU" to 0, "wrapV" to 0, "sourceLevel" to 0)) before.parameters[k] = v
        for ((k, v) in listOf("sigma" to 2.5, "spread" to 1.0, "gain" to 1.0)) before.parameters[k] = v
        val between = target(input)
        val expected = target(input)
        before.parameters["blurDirection"] = Vector2(1.0, 0.0)
        before.apply(input, between)
        before.parameters["blurDirection"] = Vector2(0.0, 1.0)
        before.apply(between, expected)
        for (x in 0 until 64 step 3) {
            val e = expected.pixel(x, 24)
            val c = target.pixel(x, 24)
            assertTrue(abs(e.r - c.r) < 1E-5 && abs(e.alpha - c.alpha) < 1E-5, "the blur at $x: $c, not $e")
        }

        // a hash blur of an image that is the same everywhere keeps it
        val flat = image { _, _ -> ColorRGBa(0.4, 0.2, 0.1, 0.5) }
        HashBlur().apply(flat, target)
        assertColor(ColorRGBa(0.4, 0.2, 0.1), target.pixel(30, 20), 1E-3, "a hash blur")
        assertEquals(0.5, target.pixel(30, 20).alpha, 1E-3)
    }
}

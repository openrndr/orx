import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadFont
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.color.mixing.PigmentReflectances
import org.openrndr.extra.color.mixing.SpectralColor
import org.openrndr.extra.color.mixing.SpectralGamutMapping
import org.openrndr.extra.color.mixing.mixSpectral
import org.openrndr.extra.color.mixing.reflectanceToRGBa
import org.openrndr.math.Vector2
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Demonstrates a paper chromatography effect with spectral color mixing.
 *
 * An ink is a mixture of dyes. When a drop of ink sits on wet paper, the solvent spreads outward and carries the dyes
 * with it, but every dye travels at its own speed, given by its retention factor: the fraction of the solvent front's
 * distance that it covers. The dyes separate into colored rings, each blurring as it travels and leaving a faint
 * trail. A tide line marks the solvent front.
 *
 * The dyes are measured spectra from [PigmentReflectances]. At every radius, the paper and the local concentrations of
 * the dyes are mixed with Kubelka–Munk theory, where bands overlap their dyes mix. The radial color profile of every
 * spot is written to a small texture; a shader draws the spot from it and warps the radius with noise along the angle,
 * for the irregular, feathered edges that paper fibers cause. Click to drop another ink.
 */
fun main() = application {
    configure {
        width = 720
        height = 720
    }
    program {
        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 12.0)

        // slightly warm filter paper
        val paperSpectrum = DoubleArray(38) { 0.80 + 0.08 * (it / 37.0) }
        val paper = SpectralColor(paperSpectrum)
        val paperColor = reflectanceToRGBa(paperSpectrum).toSRGB()
        // the residue that collects at the solvent front
        val tide = SpectralColor(PigmentReflectances.rawUmber)

        /**
         * A dye in an ink.
         * @param retention the fraction of the solvent front's distance the dye travels
         * @param amount the amount of dye in the ink
         */
        class Dye(val color: SpectralColor, val retention: Double, val amount: Double)

        class Ink(val name: String, val dyes: List<Dye>)

        val inks = listOf(
            Ink("black", listOf(
                Dye(SpectralColor(PigmentReflectances.methyleneBlue), 0.8, 1.0),
                Dye(SpectralColor(PigmentReflectances.rhodamine6G), 0.58, 1.0),
                Dye(SpectralColor(PigmentReflectances.curcuma), 0.38, 1.2)
            )),
            Ink("green", listOf(
                Dye(SpectralColor(PigmentReflectances.safflower), 0.75, 1.4),
                Dye(SpectralColor(PigmentReflectances.methyleneBlue), 0.55, 1.0)
            )),
            Ink("violet", listOf(
                Dye(SpectralColor(PigmentReflectances.rhodamine6G), 0.8, 1.0),
                Dye(SpectralColor(PigmentReflectances.methyleneBlue), 0.45, 0.8),
                Dye(SpectralColor(PigmentReflectances.tyrianPurple), 0.2, 1.5)
            )),
            Ink("brown", listOf(
                Dye(SpectralColor(PigmentReflectances.curcuma), 0.9, 1.0),
                Dye(SpectralColor(PigmentReflectances.carmineLake), 0.6, 1.0),
                Dye(SpectralColor(PigmentReflectances.prussianBlue), 0.3, 0.6)
            ))
        )

        val profileSize = 256

        class Spot(val position: Vector2, val ink: Ink, val start: Double) {
            // the radial color profile, linear RGB with alpha 0 outside the solvent front
            val profile: ColorBuffer = colorBuffer(profileSize, 1, type = ColorType.FLOAT32)
            val seed = Random.nextDouble(0.0, 100.0)
            var done = false
        }

        val maxRadius = 150.0
        // the profile covers the spot and a margin for the warped edge
        val extent = maxRadius * 1.15
        val duration = 12.0
        val spots = mutableListOf(
            Spot(Vector2(190.0, 190.0), inks[0], 0.0),
            Spot(Vector2(530.0, 190.0), inks[1], 1.5),
            Spot(Vector2(190.0, 530.0), inks[2], 3.0),
            Spot(Vector2(530.0, 530.0), inks[3], 4.5)
        )
        var next = 0
        mouse.buttonDown.listen {
            spots.add(Spot(it.position, inks[next++ % inks.size], seconds))
        }

        /**
         * The color at radius r of a spot whose solvent front is at radius front.
         */
        fun colorAt(ink: Ink, r: Double, front: Double): ColorRGBa {
            if (r > front + 4.0) return ColorRGBa.TRANSPARENT
            val parts = mutableListOf(paper to 1.0)
            for (dye in ink.dyes) {
                val center = dye.retention * front
                val width = 3.0 + 0.05 * front
                // the amount of dye is conserved, a ring of larger radius spreads it over more paper
                val band = exp(-0.5 * ((r - center) / width) * ((r - center) / width))
                val trail = if (r < center) 0.06 * exp(-(center - r) / (0.3 * front + 1.0)) else 0.0
                val concentration = dye.amount * (band + trail) * 9.0 / sqrt(center + 10.0)
                if (concentration > 1E-3) parts.add(dye.color to concentration)
            }
            val tideLine = exp(-0.5 * ((r - front) / 2.0) * ((r - front) / 2.0)) * 0.6
            if (tideLine > 1E-3) parts.add(tide to tideLine)
            return mixSpectral(*parts.toTypedArray()).toRGBa(SpectralGamutMapping.MAP)
        }

        // the spot is drawn from its profile, the radius is warped with value noise along the angle: slowly for the
        // overall shape, quickly for the feathered fibers
        val spotStyle = shadeStyle {
            fragmentPreamble = """
                float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
                float valueNoise(vec2 p) {
                    vec2 i = floor(p);
                    vec2 f = fract(p);
                    vec2 u = f * f * (3.0 - 2.0 * f);
                    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
                               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
                }
            """.trimIndent()
            fragmentTransform = """
                vec2 p = (c_boundsPosition.xy - 0.5) * 2.0 * p_extent;
                float d = length(p);
                vec2 direction = d > 0.0 ? p / d : vec2(1.0, 0.0);
                // noise on the circle around the spot, so that it is continuous all the way round
                float shape = valueNoise(direction * 2.5 + p_seed) - 0.5;
                float fibers = valueNoise(direction * 45.0 + p_seed) - 0.5;
                float fine = valueNoise(direction * 160.0 + p_seed) - 0.5;
                float warp = 1.0 + 0.07 * shape + (0.025 * fibers + 0.012 * fine) * smoothstep(0.0, 60.0, d);
                float u = (d / warp) / p_extent;
                vec4 color = u < 1.0 ? texture(p_profile, vec2(u, 0.5)) : vec4(0.0);
                x_fill = vec4(color.rgb * color.a, color.a);
            """.trimIndent()
        }

        extend {
            drawer.clear(paperColor)
            drawer.stroke = null
            for (spot in spots) {
                val t = (seconds - spot.start).coerceIn(0.0, duration)
                if (t <= 0.0) continue
                if (!spot.done) {
                    // capillary flow: the front moves with the square root of time
                    val front = maxRadius * sqrt(t / duration)
                    val shadow = spot.profile.shadow
                    for (i in 0 until profileSize) {
                        shadow[i, 0] = colorAt(spot.ink, extent * (i + 0.5) / profileSize, front)
                    }
                    shadow.upload()
                    spot.done = t >= duration
                }
                spotStyle.parameter("profile", spot.profile)
                spotStyle.parameter("extent", extent)
                spotStyle.parameter("seed", spot.seed)
                drawer.shadeStyle = spotStyle
                drawer.rectangle(spot.position.x - extent, spot.position.y - extent, 2.0 * extent, 2.0 * extent)
                drawer.shadeStyle = null
            }
            drawer.fontMap = font
            drawer.fill = ColorRGBa.fromHex("#555555")
            for (spot in spots.take(4)) {
                drawer.text(spot.ink.name, spot.position.x - 20.0, spot.position.y + maxRadius + 22.0)
            }
            drawer.text("click to drop more ink", 20.0, 24.0)
        }
    }
}

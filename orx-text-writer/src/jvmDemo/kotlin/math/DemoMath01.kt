package math

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.loadFace
import org.openrndr.extra.textwriter.SlugTextDrawer
import org.openrndr.extra.textwriter.TextStyle
import org.openrndr.extra.textwriter.math.layoutMath
import org.openrndr.extra.textwriter.math.parseMathML
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle

/**
 * Demonstrates math layout: formulas in MathML laid out by the OpenType MATH table of a math font, with fractions,
 * radicals, scripts, limits and fences that stretch. The glyphs are drawn with a [SlugTextDrawer], the bars of the
 * fractions and radicals as rectangles.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val face = loadFace("https://raw.githubusercontent.com/stipub/stixfonts/v2.13/fonts/static_otf/STIXTwoMath-Regular.otf").withSize(32.0)

            val formulas = listOf(
                """<math display="block"><mi>x</mi><mo>=</mo><mfrac><mrow><mo>-</mo><mi>b</mi><mo>±</mo><msqrt><msup><mi>b</mi><mn>2</mn></msup><mo>-</mo><mn>4</mn><mi>a</mi><mi>c</mi></msqrt></mrow><mrow><mn>2</mn><mi>a</mi></mrow></mfrac></math>""",
                """<math display="block"><munderover><mo>&sum;</mo><mrow><mi>i</mi><mo>=</mo><mn>1</mn></mrow><mi>n</mi></munderover><msup><mi>i</mi><mn>2</mn></msup><mo>=</mo><mfrac><mrow><mi>n</mi><mo>(</mo><mi>n</mi><mo>+</mo><mn>1</mn><mo>)</mo><mo>(</mo><mn>2</mn><mi>n</mi><mo>+</mo><mn>1</mn><mo>)</mo></mrow><mn>6</mn></mfrac></math>""",
                """<math><msubsup><mo>&int;</mo><mn>0</mn><mn>1</mn></msubsup><msup><mi>e</mi><mrow><mo>-</mo><msup><mi>x</mi><mn>2</mn></msup></mrow></msup><mi>d</mi><mi>x</mi><mo>,</mo><mspace width="1em"/><mroot><mrow><mi>x</mi><mo>+</mo><mn>1</mn></mrow><mn>3</mn></mroot></math>""",
                """<math display="block"><mrow><mo>(</mo><mfrac><mrow><mn>1</mn><mo>+</mo><mfrac><mn>1</mn><mi>x</mi></mfrac></mrow><mrow><mn>1</mn><mo>-</mo><mfrac><mn>1</mn><msup><mi>x</mi><mn>2</mn></msup></mfrac></mrow></mfrac><mo>)</mo></mrow><mo>=</mo><mrow><mo>[</mo><msub><mi>a</mi><mrow><mi>i</mi><mi>j</mi></mrow></msub><mo>]</mo></mrow></math>"""
            )

            val slug = SlugTextDrawer()
            val rules = mutableListOf<Rectangle>()
            val style = TextStyle(fill = ColorRGBa.BLACK)
            var top = 40.0
            for (mathml in formulas) {
                val formula = parseMathML(mathml)
                // the baseline below the top by the ascent of the formula
                val ascent = layoutMath(formula, face).ascent
                val layout = layoutMath(formula, face, origin = Vector2(40.0, top + ascent), style = style)
                slug.addGlyphs(layout.glyphs)
                rules += layout.rules
                top += layout.ascent + layout.descent + 40.0
            }

            extend {
                drawer.clear(ColorRGBa.WHITE)
                drawer.fill = ColorRGBa.BLACK
                drawer.stroke = null
                drawer.rectangles(rules)
                slug.draw(drawer)
            }
        }
    }
}

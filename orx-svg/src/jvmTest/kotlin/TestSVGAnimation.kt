import org.openrndr.extra.composition.*
import org.openrndr.extra.svg.animation
import org.openrndr.extra.svg.evaluate
import org.openrndr.extra.svg.parseSVG
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSVGAnimation {
    private fun parse(content: String) = parseSVG(
        """<svg xmlns="http://www.w3.org/2000/svg" width="400" height="200">$content</svg>"""
    )

    private fun Composition.shape(id: String) = findShapes().first { it.id == id }
    private fun Composition.fill(id: String) = (shape(id).effectiveStyle.fill as Paint.RGB).value.toSRGB()
    private fun Composition.opacity(id: String) = (shape(id).style.opacity as Numeric.Rational).value

    @Test
    fun interpolatesValues() {
        val c = parse(
            """
            <rect id="a" width="10" height="10" fill="#000000" opacity="1">
                <animate attributeName="fill" from="#000000" to="#ff0000" dur="2s"/>
                <animate attributeName="opacity" values="1;0;1" keyTimes="0;0.25;1" dur="4s" fill="freeze"/>
            </rect>
            """
        )
        c.evaluate(1.0)
        assertEquals(0.5, c.fill("a").r, 0.01)
        assertEquals(0.0, c.opacity("a"), 1E-9)
        c.evaluate(2.5)
        // the fill animation is over and not frozen, the opacity is halfway from 0 to 1
        assertEquals(0.0, c.fill("a").r, 1E-9)
        assertEquals(0.5, c.opacity("a"), 1E-9)
        c.evaluate(10.0)
        assertEquals(1.0, c.opacity("a"), 1E-9)
    }

    @Test
    fun animatesGeometryAndTransforms() {
        val c = parse(
            """
            <circle id="c" cx="50" cy="50" r="10">
                <animate attributeName="r" from="10" to="30" dur="2s" fill="freeze"/>
            </circle>
            <rect id="r" width="10" height="10">
                <animateTransform attributeName="transform" type="translate" by="10 0" dur="1s" additive="sum" accumulate="sum" repeatCount="3" fill="freeze"/>
            </rect>
            <rect id="s" width="10" height="10"><set attributeName="visibility" to="hidden" begin="1s" dur="1s"/></rect>
            """
        )
        c.evaluate(1.0)
        assertEquals(40.0, c.shape("c").shape.bounds.width, 1E-6)
        c.evaluate(5.0)
        assertEquals(60.0, c.shape("c").shape.bounds.width, 1E-6)
        // three repeats of 10, accumulated
        assertEquals(30.0, c.shape("r").effectiveShape.bounds.x, 1E-6)
        c.evaluate(1.5)
        assertEquals(15.0, c.shape("r").effectiveShape.bounds.x, 1E-6)
        assertEquals(Visibility.Hidden, c.shape("s").effectiveStyle.visibility)
        c.evaluate(2.5)
        assertEquals(Visibility.Visible, c.shape("s").effectiveStyle.visibility)
    }

    @Test
    fun movesAlongPaths() {
        val c = parse(
            """
            <rect id="m" x="-1" y="-1" width="2" height="2">
                <animateMotion path="M 0 0 L 100 0" dur="2s" fill="freeze"/>
            </rect>
            """
        )
        c.evaluate(0.5)
        assertEquals(Vector2(25.0, 0.0), c.shape("m").effectiveShape.bounds.center)
    }

    @Test
    fun beginsWithEventsAndOtherAnimations() {
        val c = parse(
            """
            <rect id="b" width="10" height="10" opacity="1">
                <animate id="first" attributeName="opacity" to="0" dur="1s" begin="click" fill="freeze"/>
            </rect>
            <rect id="d" width="10" height="10" opacity="1">
                <animate attributeName="opacity" to="0" dur="1s" begin="first.end" fill="freeze"/>
            </rect>
            """
        )
        c.evaluate(1.0)
        assertEquals(1.0, c.opacity("b"), 1E-9)
        val animation = c.animation!!
        animation.dispatch(animation.element(c.shape("b"))!!, "click")
        c.evaluate(1.5)
        assertEquals(0.5, c.opacity("b"), 1E-9)
        assertEquals(1.0, c.opacity("d"), 1E-9)
        c.evaluate(2.5)
        assertEquals(0.0, c.opacity("b"), 1E-9)
        assertEquals(0.5, c.opacity("d"), 1E-9)
    }

    @Test
    fun runsCssAnimations() {
        val c = parse(
            """
            <style>
                @keyframes fade { from { opacity: 1 } 50% { opacity: 0.2 } to { opacity: 0.6 } }
                .a { animation: fade 2s linear 1s 2 alternate both; }
            </style>
            <rect id="a" class="a" width="10" height="10"/>
            """
        )
        // before the delay the first keyframe applies, filling backwards
        c.evaluate(0.5)
        assertEquals(1.0, c.opacity("a"), 1E-9)
        c.evaluate(1.5)
        assertEquals(0.6, c.opacity("a"), 1E-9)
        // the second iteration runs backwards: at 75% of the keyframes, halfway from 0.2 to 0.6
        c.evaluate(3.5)
        assertEquals(0.4, c.opacity("a"), 1E-9)
        c.evaluate(10.0)
        assertEquals(1.0, c.opacity("a"), 1E-9)
    }

    @Test
    fun transitionsBetweenPointerStates() {
        val c = parse(
            """
            <style>
                .b { fill: #000000; transition: fill 1s linear; }
                .b:hover { fill: #ff0000; }
                g:hover .inner { opacity: 0.5; }
            </style>
            <rect id="b" class="b" width="10" height="10"/>
            <g id="g"><rect id="inner" class="inner" width="10" height="10"/></g>
            """
        )
        val animation = c.animation!!
        c.evaluate(0.0)
        assertEquals(0.0, c.fill("b").r, 1E-9)
        animation.setHovered(animation.element(c.shape("b"))!!, true)
        animation.setHovered(animation.element(c.root.findGroups().first { it.id == "g" })!!, true)
        c.evaluate(1.0)
        c.evaluate(1.25)
        assertEquals(0.25, c.fill("b").r, 0.01)
        // without a transition the value changes at once, the rule depends on the hover of the group
        assertEquals(0.5, c.opacity("inner"), 1E-9)
        c.evaluate(3.0)
        assertEquals(1.0, c.fill("b").r, 1E-9)
    }

    @Test
    fun transitionsChangesOfStylesByPrograms() {
        val c = parse(
            """
            <style>
                .b { fill: #000000; opacity: 1; transition: fill 1s linear, opacity 2s linear; }
                .b.on { fill: #ff0000; }
            </style>
            <rect id="b" class="b" width="10" height="10"/>
            """
        )
        val animation = c.animation!!
        c.evaluate(0.0)
        // a change of the style of a node
        c.shape("b").style.opacity = Numeric.Rational(0.0)
        c.evaluate(1.0)
        assertEquals(1.0, c.opacity("b"), 1E-9)
        c.evaluate(2.0)
        assertEquals(0.5, c.opacity("b"), 1E-9)
        c.evaluate(3.0)
        assertEquals(0.0, c.opacity("b"), 1E-9)

        // a change of a property of the style that has no transition
        c.shape("b").style.strokeWeight = Length.Pixels(4.0)
        c.evaluate(3.1)
        assertEquals(4.0, (c.shape("b").style.strokeWeight as Length.Pixels).value, 1E-9)

        // a class that a rule matches
        animation.addClass(c.shape("b"), "on")
        c.evaluate(3.6)
        assertEquals(0.5, c.fill("b").r, 0.01)
        // reversing midway starts from the value that is shown
        animation.removeClass(c.shape("b"), "on")
        c.evaluate(4.1)
        assertEquals(0.25, c.fill("b").r, 0.01)
        c.evaluate(5.0)
        assertEquals(0.0, c.fill("b").r, 1E-9)

        // an inline property
        animation.setProperty(c.shape("b"), "fill", "#ffffff")
        c.evaluate(5.75)
        assertEquals(0.75, c.fill("b").g, 0.01)
        c.evaluate(7.0)
        assertEquals(1.0, c.fill("b").g, 1E-9)
        animation.setProperty(c.shape("b"), "fill", null)
        c.evaluate(9.0)
        assertEquals(0.0, c.fill("b").g, 1E-9)
    }

    @Test
    fun transitionsTransformsAroundTheirBoxes() {
        val c = parse(
            """
            <style>
                .t { transform-box: fill-box; transform-origin: center; transition: transform 1s cubic-bezier(0, 0, 1, 1); }
                .t.on { transform: rotate(90deg); }
            </style>
            <rect id="t" class="t" x="100" y="20" width="40" height="20"/>
            """
        )
        c.evaluate(0.0)
        c.animation!!.addClass(c.shape("t"), "on")
        // halfway, from none in the units of the transform, around the center of the rectangle
        c.evaluate(0.5)
        val center = Vector2(120.0, 30.0)
        assertEquals(center, (c.shape("t").transform * center.xy01).xy)
        assertEquals(kotlin.math.sqrt(0.5), c.shape("t").transform.c0r0, 1E-6)
        c.evaluate(1.0)
        assertEquals(0.0, c.shape("t").transform.c0r0, 1E-6)
        assertEquals(center, (c.shape("t").transform * center.xy01).xy)
    }

    @Test
    fun documentsWithoutAnimationHaveNone() {
        assertNull(parse("""<rect width="10" height="10"/>""").animation)
        assertTrue(parse("""<rect width="10" height="10"><animate attributeName="x" to="5" dur="1s"/></rect>""").animation != null)
    }
}

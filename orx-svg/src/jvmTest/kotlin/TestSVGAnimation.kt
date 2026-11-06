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
    fun sendsEventsAndControlsPlayback() {
        val c = parse(
            """
            <style>
                @keyframes pulse { to { opacity: 0; } }
                .p { animation: pulse 1s linear 0.5s 2; }
                .t { fill: #000000; transition: fill 1s linear 0.25s; }
                .t.on { fill: #ffffff; }
            </style>
            <rect id="p" class="p" width="10" height="10"/>
            <rect id="t" class="t" width="10" height="10"/>
            <rect id="s" width="10" height="10">
                <animate id="a" attributeName="x" from="0" to="10" dur="1s" begin="1s" repeatCount="2"/>
                <animate id="b" attributeName="y" from="0" to="10" dur="1s" begin="a.endEvent"/>
            </rect>
            """
        )
        val animation = c.animation!!
        val events = mutableListOf<String>()
        animation.animationStarted.listen { events.add("css start ${it.name} ${it.time}") }
        animation.animationIterated.listen { events.add("css iteration ${it.name} ${it.iteration} ${it.time}") }
        animation.animationEnded.listen { events.add("css end ${it.name} ${it.time}") }
        animation.began.listen { events.add("smil begin ${it.name} ${it.time}") }
        animation.repeated.listen { events.add("smil repeat ${it.name} ${it.iteration} ${it.time}") }
        animation.ended.listen { events.add("smil end ${it.name} ${it.time}") }
        animation.transitionStarted.listen { events.add("transition start ${it.name} ${it.time}") }
        animation.transitionEnded.listen { events.add("transition end ${it.name} ${it.time}") }
        animation.transitionCanceled.listen { events.add("transition cancel ${it.name} ${it.time}") }

        for (i in 0..40) {
            val t = i * 0.125
            if (t == 0.5) animation.addClass(c.shape("t"), "on")
            if (t == 1.0) animation.removeClass(c.shape("t"), "on")
            c.evaluate(t)
        }
        assertEquals(
            listOf(
                // classes change at the time of the last update, 0.375 and 0.875; transitions start after their delay
                "css start pulse 0.5",
                "transition start fill 0.75",
                "transition cancel fill 0.875",
                "smil begin a 1.0",
                "transition start fill 1.25",
                "css iteration pulse 1 1.5",
                "smil repeat a 1 2.0",
                "transition end fill 2.125",
                "css end pulse 2.5",
                "smil end a 3.0",
                "smil begin b 3.0",
                "smil end b 4.0"
            ), events
        )

        // the time of the document stops while paused, and advances at the playback rate
        animation.seek(1.5)
        c.evaluate(10.0)
        assertEquals(5.0, c.shape("s").shape.bounds.x, 1E-9)
        animation.pause()
        c.evaluate(11.0)
        assertEquals(5.0, c.shape("s").shape.bounds.x, 1E-9)
        animation.play()
        animation.playbackRate = 0.5
        c.evaluate(11.5)
        assertEquals(1.75, animation.time, 1E-9)
        assertEquals(7.5, c.shape("s").shape.bounds.x, 1E-9)
    }

    @Test
    fun beginsSmilAnimationsFromPrograms() {
        val c = parse(
            """<rect id="s" width="10" height="10"><animate id="a" attributeName="x" from="0" to="10" dur="1s" begin="indefinite" fill="freeze"/></rect>"""
        )
        c.evaluate(0.0)
        c.evaluate(2.0)
        assertEquals(0.0, c.shape("s").shape.bounds.x, 1E-9)
        c.animation!!.beginElement("a")
        c.evaluate(2.5)
        assertEquals(5.0, c.shape("s").shape.bounds.x, 1E-9)
        c.animation!!.endElement("a")
        c.evaluate(3.0)
        assertEquals(5.0, c.shape("s").shape.bounds.x, 1E-9)
    }

    private fun Composition.origin(id: String) = (shape(id).transform * Vector2.ZERO.xy01).xy
    private fun assertNear(expected: Vector2, actual: Vector2) {
        assertEquals(expected.x, actual.x, 1E-6)
        assertEquals(expected.y, actual.y, 1E-6)
    }

    @Test
    fun appliesIndividualTransforms() {
        val c = parse(
            """
            <rect id="a" width="10" height="10" style="translate: 100px 50px; rotate: 90deg; scale: 2"/>
            <rect id="b" width="10" height="10" transform="translate(5 0)" style="translate: 100px 0px"/>
            <rect id="c" x="0" y="0" width="20" height="10" style="transform-box: fill-box; transform-origin: center; rotate: 180deg"/>
            """
        )
        // translate, then rotate, then scale
        assertNear(Vector2(100.0, 50.0), c.origin("a"))
        assertNear(Vector2(100.0, 70.0), (c.shape("a").transform * Vector2(10.0, 0.0).xy01).xy)
        // before the transform
        assertNear(Vector2(105.0, 0.0), c.origin("b"))
        // around the origin
        assertNear(Vector2(20.0, 10.0), c.origin("c"))
    }

    @Test
    fun animatesIndividualTransformsFromNone() {
        val c = parse(
            """
            <style>
                @keyframes turn { to { rotate: 90deg; translate: 100px; } }
                .r { animation: turn 1s linear forwards; }
            </style>
            <rect id="r" class="r" width="10" height="10"/>
            """
        )
        c.evaluate(0.5)
        assertNear(Vector2(50.0, 0.0), c.origin("r"))
        assertNear(Vector2(50.0 + 10.0 * kotlin.math.sqrt(0.5), 10.0 * kotlin.math.sqrt(0.5)), (c.shape("r").transform * Vector2(10.0, 0.0).xy01).xy)
        c.evaluate(2.0)
        assertNear(Vector2(100.0, 10.0), (c.shape("r").transform * Vector2(10.0, 0.0).xy01).xy)
    }

    @Test
    fun movesAlongMotionPaths() {
        val c = parse(
            """
            <style>
                @keyframes along { to { offset-distance: 100%; } }
                .m { offset-path: path("M 0 0 L 100 0 L 100 100"); animation: along 2s linear forwards; }
            </style>
            <rect id="m" class="m" width="10" height="10"/>
            <rect id="f" width="10" height="10" style="offset-path: path('M 0 0 L 0 100'); offset-distance: 25%; offset-rotate: 0deg"/>
            <rect id="anchor" width="10" height="10" style="offset-path: path('M 0 0 L 100 0'); offset-distance: 50px; offset-anchor: 5px 5px"/>
            """
        )
        // a static position, without turning
        assertNear(Vector2(0.0, 25.0), c.origin("f"))
        assertNear(Vector2(10.0, 25.0), (c.shape("f").transform * Vector2(10.0, 0.0).xy01).xy)
        // the anchor is at the point on the path
        assertNear(Vector2(50.0, 0.0), (c.shape("anchor").transform * Vector2(5.0, 5.0).xy01).xy)
        // turned with the path
        c.evaluate(0.5)
        assertNear(Vector2(50.0, 0.0), c.origin("m"))
        assertNear(Vector2(60.0, 0.0), (c.shape("m").transform * Vector2(10.0, 0.0).xy01).xy)
        c.evaluate(1.5)
        assertNear(Vector2(100.0, 50.0), c.origin("m"))
        assertNear(Vector2(100.0, 60.0), (c.shape("m").transform * Vector2(10.0, 0.0).xy01).xy)
    }

    @Test
    fun morphsPathsWithTheDProperty() {
        val c = parse(
            """
            <style>
                @keyframes morph { to { d: path("M 0 0 L 100 0 L 100 100 Z"); } }
                .m { animation: morph 1s linear forwards; }
                .t { d: path("M 0 0 L 10 0 L 10 10 Z"); transition: d 1s linear; }
                .t.big { d: path("M 0 0 L 30 0 L 30 30 Z"); }
            </style>
            <path id="m" class="m" d="M 0 0 L 50 0 L 50 50 Z"/>
            <path id="t" class="t" d="M 0 0 L 99 0 L 99 99 Z"/>
            """
        )
        // the property overrides the attribute
        assertEquals(10.0, c.shape("t").shape.bounds.width, 1E-9)
        c.evaluate(0.5)
        assertEquals(75.0, c.shape("m").shape.bounds.width, 1E-9)
        c.animation!!.addClass(c.shape("t"), "big")
        c.evaluate(1.0)
        assertEquals(20.0, c.shape("t").shape.bounds.width, 1E-9)
        c.evaluate(2.0)
        assertEquals(100.0, c.shape("m").shape.bounds.width, 1E-9)
        assertEquals(30.0, c.shape("t").shape.bounds.width, 1E-9)
        c.animation!!.removeClass(c.shape("t"), "big")
        c.evaluate(2.5)
        assertEquals(20.0, c.shape("t").shape.bounds.width, 1E-9)
    }

    @Test
    fun documentsWithoutAnimationHaveNone() {
        assertNull(parse("""<rect width="10" height="10"/>""").animation)
        assertTrue(parse("""<rect width="10" height="10"><animate attributeName="x" to="5" dur="1s"/></rect>""").animation != null)
    }
}

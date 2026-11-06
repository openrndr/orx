import org.openrndr.KEY_ARROW_DOWN
import org.openrndr.KEY_ARROW_LEFT
import org.openrndr.KEY_ARROW_RIGHT
import org.openrndr.KEY_ARROW_UP
import org.openrndr.KEY_SPACEBAR
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.extra.composition.CompositionInteraction
import org.openrndr.extra.composition.composition
import org.openrndr.extra.svg.SVGAnimationEvent
import org.openrndr.extra.svg.animation
import org.openrndr.extra.svg.evaluate
import org.openrndr.extra.svg.loadSVG

/**
 * Demonstrates CSS motion paths, individual transforms, the d property, and the events and playback control of
 * animations:
 *
 * - a car and a follower driven along a track with offset-path and an animated offset-distance; the car turns with
 *   the track, the follower keeps upright with offset-rotate: 0deg
 * - boxes animated with the rotate, scale and translate properties, which combine with their transforms
 * - a star that morphs into a decagon with @keyframes of d: path(...); clicking it toggles a class that stops the
 *   morph and transitions d to the decagon
 * - clicking the car begins a SMIL animation with beginElement, a second animation begins with its begin event
 * - space pauses and plays, the left and right arrows seek, the up and down arrows change the playback rate; the
 *   events of the animations are listed
 */
fun main() = application {
    configure {
        width = 720
        height = 480
    }
    program {
        val composition = loadSVG("orx-svg/src/jvmDemo/resources/svg/motion.svg")
        val animation = composition.animation!!
        val interaction = CompositionInteraction(composition).apply { listenTo(mouse) }
        animation.listenTo(interaction)
        interaction.clicked.listen {
            when (animation.element(it.node)?.id()) {
                "car" -> animation.beginElement("honk")
                "star" -> animation.toggleClass(it.node, "calm")
            }
        }

        keyboard.keyDown.listen {
            when (it.key) {
                KEY_SPACEBAR -> if (animation.paused) animation.play() else animation.pause()
                KEY_ARROW_LEFT -> animation.seek(animation.time - 1.0)
                KEY_ARROW_RIGHT -> animation.seek(animation.time + 1.0)
                KEY_ARROW_UP -> animation.playbackRate *= 2.0
                KEY_ARROW_DOWN -> animation.playbackRate /= 2.0
            }
        }

        // the most recent events, except the iterations of the endless animations
        val log = ArrayDeque<String>()
        fun record(kind: String) = { e: SVGAnimationEvent ->
            log.addFirst("%6.2f  %-20s %s".format(e.time, kind, e.name.ifEmpty { e.element.tagName() }))
            while (log.size > 6) log.removeLast()
        }
        animation.began.listen(record("began"))
        animation.ended.listen(record("ended"))
        animation.animationStarted.listen(record("animation started"))
        animation.transitionStarted.listen(record("transition started"))
        animation.transitionEnded.listen(record("transition ended"))

        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 12.0)
        extend {
            composition.evaluate(seconds)
            drawer.clear(ColorRGBa.WHITE)
            drawer.composition(composition)
            drawer.fontMap = font
            drawer.fill = ColorRGBa.fromHex("#202020")
            drawer.text("time %.2f  rate %.2f%s".format(animation.time, animation.playbackRate, if (animation.paused) "  paused" else ""), 20.0, 400.0)
            log.forEachIndexed { i, line -> drawer.text(line, 380.0, 400.0 + i * 14.0) }
        }
    }
}

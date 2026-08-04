import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.processing.toShape
import org.openrndr.math.IntVector2
import org.openrndr.shape.Shape
import processing.core.PApplet
import processing.core.PShape

private const val winWidth = 320
private const val winHeight = 640

class P5 : PApplet() {
    val pShapes = mutableListOf<PShape>()
    override fun settings() {
        size(winWidth, winHeight)
    }

    fun init() = runSketch()

    fun shapeClosed(): PShape {
        val s = createShape()
        s.beginShape()
        s.fill(255)
        s.stroke(0)
        s.vertex(170f, 20f)
        s.vertex(170f, 70f)
        s.vertex(220f, 70f)
        s.vertex(220f, 20f)
        s.endShape(CLOSE)
        return s
    }

    fun shapeOpen(): PShape {
        val s = createShape()
        s.beginShape()
        s.fill(255)
        s.stroke(0)
        s.vertex(100f, 20f)
        s.vertex(100f, 70f)
        s.vertex(150f, 70f)
        s.vertex(150f, 20f)
        s.endShape()
        return s
    }

    // Not implemented yet
    fun shapeRect(): PShape = createShape(RECT, 0f, 0f, 50f, 50f)

    // Not implemented yet
    fun shapeEllipse(): PShape = createShape(ELLIPSE, 50f, 50f, 80f, 50f)

    // Not implemented yet
    fun shapeArc(): List<PShape> = List(10) {
        val t = it / 9f
        val r = 20f + 200f * t
        noFill()
        createShape(
            ARC, 100f, 200f, r, r * 0.5f,
            TWO_PI * t, TWO_PI * (t + 0.5f),
            1f // PIE=3f, CHORD=2f, OPEN=1f
        )
    }

    // Copied from the Processing reference
    fun shapeAlien(): PShape {
        val s = createShape(GROUP)
        val head = createShape(ELLIPSE, 0f, 25f, 50f, 50f)
        head.setFill(color(255))
        val body = createShape(RECT, -25f, 45f, 50f, 40f)
        body.setFill(color(0))
        s.addChild(body)
        s.addChild(head)
        return s
    }

    fun shapeGroup(): PShape {
        val s = createShape(GROUP)
        val a = createShape(QUAD, 50f, 350f, 100f, 400f, 50f, 450f, 20f, 400f)
        val b = createShape(QUAD, 70f, 350f, 120f, 400f, 70f, 450f, 40f, 400f)
        s.addChild(a)
        s.addChild(b)
        return s
    }

    // Shapes with overlapping parts are rendered different in
    // OPENRNDR and Processing. To get the same look in OPENRNDR,
    // the parts can be converted to ShapeContours instead of to a Phape.
    fun shapeHole(): PShape {
        val s = createShape()
        s.beginShape()
        s.fill(255)
        s.stroke(0)
        s.vertex(170f, 320f)
        s.vertex(170f, 370f)
        s.vertex(220f, 370f)
        s.vertex(220f, 320f)
        s.beginContour()
        s.vertex(210f, 330f)
        s.vertex(210f, 360f)
        s.vertex(180f, 360f)
        s.vertex(180f, 330f)
        s.endContour()
        s.endShape(CLOSE)
        return s
    }

    fun shapeQuad(): PShape = createShape(
        QUAD,
        50f, 250f,
        100f, 300f,
        50f, 350f,
        20f, 300f
    )

    fun shapeTriangle(): PShape = createShape(
        TRIANGLE,
        150f, 250f,
        200f, 300f,
        150f, 350f
    )

    fun shapeLine(): PShape = createShape(
        LINE,
        50f, 50f,
        winWidth - 50f, winHeight - 50f
    )

    override fun setup() {
        pShapes.add(shapeClosed())
        pShapes.add(shapeOpen())
        pShapes.add(shapeQuad())
        pShapes.add(shapeTriangle())
        pShapes.add(shapeLine())
        pShapes.add(shapeHole())
        pShapes.add(shapeGroup())
        //pShapes.addAll(shapeArc())
        //ss.add(shapeRect())
        //ss.add(shapeEllipse())
        //ss.add(shapeAlien())
    }

    override fun draw() {
        background(color(90, 120, 240))
        pShapes.forEach { shape(it) }
    }
}

/**
 * This program creates two windows: one rendered with OPENRNDR
 * and another rendered with Processing.
 *
 * The Processing application creates and renders a collection
 * of `PShape` instances.
 *
 * The OPENRNDR converts the collection of `PShape` instances
 * to `Shape` on its 5th animation frame, giving the
 * Processing program time to initialize and run.
 */
fun main() = application {
    configure {
        width = winWidth
        height = winHeight
        val dim = displays.first().dimensions!!
        position = IntVector2(dim.x / 2 + width / 2 + 1, dim.y / 2 - height / 2 - 1)
    }
    program {
        val p5 = P5()
        p5.init()

        val shapes = mutableListOf<Shape>()

        extend {
            if (frameCount == 5) {
                shapes.addAll(p5.pShapes.map { it.toShape() })
            }
            // Quit after 30 frames on CI
            if (System.getProperty("takeScreenshot") == "true" && frameCount == 30) {
                p5.exit()
            }
            drawer.clear(ColorRGBa.PINK)
            drawer.fill = ColorRGBa.WHITE
            drawer.stroke = ColorRGBa.BLACK
            drawer.shapes(shapes)
        }
    }
}
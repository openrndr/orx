import org.amshove.kluent.`should be equal to`
import org.openrndr.extra.shapes.operators.bevelCorners
import org.openrndr.extra.shapes.operators.roundCorners
import org.openrndr.extra.shapes.regularPolygon
import org.openrndr.shape.Circle
import org.openrndr.shape.contour
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestChamferCorners : Spek({

    describe("a single segment linear contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
        }

        it("should be similar to a chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size `should be equal to` 1
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed `should be equal to` c.closed
        }
    }

    describe("a single segment quadratic contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(40.0, 40.0, 100.0, 100.0)
        }

        it("should be similar to a chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size `should be equal to` 1
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(0.5) `should be near` c.position(0.5)
            cc.position(1.0) `should be near` c.position(1.0)
        }
    }

    describe("a circle contour") {
        val c = Circle(0.0, 0.0, 200.0).contour

        it("should be similar to a chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size `should be equal to` c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(0.5) `should be near` c.position(0.5)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed `should be equal to` c.closed
        }
    }

    describe("a two segment linear contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(50.0, 50.0)
            lineTo(100.0, 50.0)
        }
        it("should chamfer correctly") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size `should be equal to` 3
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed `should be equal to` c.closed
        }
    }

    describe("a two segment linear-curve contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(50.0, 50.0)
            curveTo(80.0, 120.0, 100.0, 50.0)
        }
        it("should be identical to the chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size `should be equal to` c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed `should be equal to` c.closed
        }
    }

    describe("a two segment curve-linear contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(80.0, 120.0, 50.0, 50.0)
            lineTo(100.0, 50.0)

        }
        it("should be identical to the chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size `should be equal to` c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed `should be equal to` c.closed
        }
    }

    describe("a two segment curve-linear contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(80.0, 120.0, 50.0, 50.0)
            lineTo(100.0, 50.0)

        }
        it("should be identical to the chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size `should be equal to` c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed `should be equal to` c.closed
        }
    }

    describe("a triangle") {
        val c = regularPolygon(3, radius = 100.0)

        c.closed `should be equal to` true

        val cc = c.roundCorners(1.0)

        c.closed `should be equal to` cc.closed

        val ccc = cc.roundCorners(1.0)

        ccc.closed `should be equal to` cc.closed

        cc.segments.size `should be equal to` 6

        cc.segments.forEach {
            println(it)
        }

        println("---")
        ccc.segments.forEach {
            println(it)
        }
        it("should have 6 sides") {
            ccc.segments.size `should be equal to` cc.segments.size
        }
        it("should start at the right position") {
            ccc.position(0.0) `should be near`  cc.position(0.0)
        }
        it("should end at the right position") {
            ccc.position(1.0) `should be near`  cc.position(1.0)
        }



    }
})
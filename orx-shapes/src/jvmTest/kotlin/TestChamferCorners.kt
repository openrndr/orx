import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import org.openrndr.extra.shapes.operators.bevelCorners
import org.openrndr.extra.shapes.operators.roundCorners
import org.openrndr.extra.shapes.regularPolygon
import org.openrndr.shape.Circle
import org.openrndr.shape.contour

class TestChamferCorners : DescribeSpec({

    describe("a single segment linear contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
        }

        it("should be similar to a chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size shouldBeEqual 1
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed shouldBeEqual c.closed
        }
    }

    describe("a single segment quadratic contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(40.0, 40.0, 100.0, 100.0)
        }

        it("should be similar to a chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size shouldBeEqual 1
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(0.5) `should be near` c.position(0.5)
            cc.position(1.0) `should be near` c.position(1.0)
        }
    }

    describe("a circle contour") {
        val c = Circle(0.0, 0.0, 200.0).contour

        it("should be similar to a chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size shouldBeEqual c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(0.5) `should be near` c.position(0.5)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed shouldBeEqual c.closed
            c.winding shouldBeEqual cc.winding
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
            cc.segments.size shouldBeEqual 3
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed shouldBeEqual c.closed
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
            cc.segments.size shouldBeEqual c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed shouldBeEqual c.closed
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
            cc.segments.size shouldBeEqual c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed shouldBeEqual c.closed
        }
    }

    describe("another two segment curve-linear contour") {
        val c = contour {
            moveTo(0.0, 0.0)
            curveTo(80.0, 120.0, 50.0, 50.0)
            lineTo(100.0, 50.0)

        }
        it("should be identical to the chamfered version") {
            val cc = c.bevelCorners(10.0)
            cc.segments.size shouldBeEqual c.segments.size
            cc.position(0.0) `should be near` c.position(0.0)
            cc.position(1.0) `should be near` c.position(1.0)
            cc.closed shouldBeEqual c.closed
        }
    }

    describe("a triangle") {
        val c = regularPolygon(3, radius = 100.0)

        c.closed shouldBeEqual true

        val cc = c.roundCorners(1.0)

        c.winding shouldBeEqual cc.winding

        c.closed shouldBeEqual cc.closed

        val ccc = cc.roundCorners(1.0)

        ccc.closed shouldBeEqual cc.closed

        cc.segments.size shouldBeEqual 6

//        cc.segments.forEach {
//            println(it)
//        }

//        println("---")
//        ccc.segments.forEach {
//            println(it)
//        }
        it("should have 6 sides") {
            ccc.segments.size shouldBeEqual cc.segments.size
        }
        it("should start at the right position") {
            ccc.position(0.0) `should be near`  cc.position(0.0)
        }
        it("should end at the right position") {
            ccc.position(1.0) `should be near`  cc.position(1.0)
        }



    }
})
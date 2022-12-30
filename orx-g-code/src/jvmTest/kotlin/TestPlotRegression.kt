package test

import org.openrndr.extra.gcode.Plot
import org.openrndr.extra.gcode.basicGrblSetup
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.openrndr.math.Vector2
import kotlin.test.assertEquals


object TestPlotRegression : Spek({

    describe("plot with grbl generator") {


        it("single file with circle") {
            val plot = Plot(dimensions = Vector2(210.0, 297.0),generator = basicGrblSetup())
            plot.draw {
                circle(100.0, 100.0, 30.0)
            }

            val expected = """
                G21
                G90
                ;begin composition
                ;begin shape: 0
                G0 X70.0 Y100.0
                M3 S255
                G1 X70.0 Y100.0 F500.0
                G1 X70.105 Y96.918 F500.0
                G1 X71.304 Y91.059 F500.0
                G1 X73.581 Y85.675 F500.0
                G1 X76.816 Y80.887 F500.0
                G1 X80.887 Y76.816 F500.0
                G1 X85.675 Y73.581 F500.0
                G1 X91.059 Y71.304 F500.0
                G1 X96.918 Y70.105 F500.0
                G1 X100.0 Y70.0 F500.0
                G1 X103.082 Y70.105 F500.0
                G1 X108.941 Y71.304 F500.0
                G1 X114.325 Y73.581 F500.0
                G1 X119.113 Y76.816 F500.0
                G1 X123.184 Y80.887 F500.0
                G1 X126.419 Y85.675 F500.0
                G1 X128.696 Y91.059 F500.0
                G1 X129.895 Y96.918 F500.0
                G1 X130.0 Y100.0 F500.0
                G1 X129.895 Y103.082 F500.0
                G1 X128.696 Y108.941 F500.0
                G1 X126.419 Y114.325 F500.0
                G1 X123.184 Y119.113 F500.0
                G1 X119.113 Y123.184 F500.0
                G1 X114.325 Y126.419 F500.0
                G1 X108.941 Y128.696 F500.0
                G1 X103.082 Y129.895 F500.0
                G1 X100.0 Y130.0 F500.0
                G1 X96.918 Y129.895 F500.0
                G1 X91.059 Y128.696 F500.0
                G1 X85.675 Y126.419 F500.0
                G1 X80.887 Y123.184 F500.0
                G1 X76.816 Y119.113 F500.0
                G1 X73.581 Y114.325 F500.0
                G1 X71.304 Y108.941 F500.0
                G1 X70.105 Y103.082 F500.0
                G1 X70.0 Y100.0 F500.0
                M3 S0
                ;end shape: 0
                ;end composition
                G0 X0 Y0
                G90
                
            """.trimIndent()

            assertEquals(expected, plot.toCombinedGcode())
        }

        describe("multiple layers") {
            val plot = Plot(dimensions = Vector2(210.0, 297.0),generator = basicGrblSetup())
            plot.draw {
                lineSegment(0.0, 20.0, 100.0, 200.0)
                lineSegment(0.5, 20.5, 100.5, 200.5)
            }
            plot.layer("rect") {
                rectangle(10.1234, 30.1234, 50.1234, 70.1234)
            }
            plot.layer("dot") {
                lineSegment(25.55555, 35.55555,25.55555, 35.55555 )
            }

            it("multi file") {

                val default = """
                   G21
                   G90
                   ;begin composition
                   ;begin shape: 0
                   G0 X0.0 Y20.0
                   M3 S255
                   G1 X100.0 Y200.0 F500.0
                   M3 S0
                   ;end shape: 0
                   ;begin shape: 1
                   G0 X0.5 Y20.5
                   M3 S255
                   G1 X100.5 Y200.5 F500.0
                   M3 S0
                   ;end shape: 1
                   ;end composition
                   G0 X0 Y0
                   G90
                   
                    """.trimIndent()
                val rect = """
                    G21
                    G90
                    ;begin composition
                    ;begin shape: 0
                    G0 X10.123 Y30.123
                    M3 S255
                    G1 X60.247 Y30.123 F500.0
                    G1 X60.247 Y100.247 F500.0
                    G1 X10.123 Y100.247 F500.0
                    G1 X10.123 Y30.123 F500.0
                    M3 S0
                    ;end shape: 0
                    ;end composition
                    G0 X0 Y0
                    G90
                    
                    """.trimIndent()

                val dot = """
                    G21
                    G90
                    ;begin composition
                    ;begin shape: 0
                    G0 X25.556 Y35.556
                    M3 S255
                    ;dot
                    M3 S0
                    ;end shape: 0
                    ;end composition
                    G0 X0 Y0
                    G90
                   
                    """.trimIndent()

                val got = plot.toSplitGcode()
                assertEquals(default, got["default"])
                assertEquals(rect, got["rect"])
                assertEquals(dot, got["dot"])
            }

            it("single file") {
                val expected = """
                   G21
                   G90
                   ;begin composition
                   ;begin shape: 0
                   G0 X0.0 Y20.0
                   M3 S255
                   G1 X100.0 Y200.0 F500.0
                   M3 S0
                   ;end shape: 0
                   ;begin shape: 1
                   G0 X0.5 Y20.5
                   M3 S255
                   G1 X100.5 Y200.5 F500.0
                   M3 S0
                   ;end shape: 1
                   ;end composition
                   ;begin composition
                   ;begin shape: 0
                   G0 X10.123 Y30.123
                   M3 S255
                   G1 X60.247 Y30.123 F500.0
                   G1 X60.247 Y100.247 F500.0
                   G1 X10.123 Y100.247 F500.0
                   G1 X10.123 Y30.123 F500.0
                   M3 S0
                   ;end shape: 0
                   ;end composition
                   ;begin composition
                   ;begin shape: 0
                   G0 X25.556 Y35.556
                   M3 S255
                   ;dot
                   M3 S0
                   ;end shape: 0
                   ;end composition
                   G0 X0 Y0
                   G90
                   
                    """.trimIndent()
                assertEquals(expected, plot.toCombinedGcode())
            }

        }
    }
})
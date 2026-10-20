package test


import org.openrndr.extra.gcode.BasicGrblGenerator
import org.openrndr.extra.gcode.Plot
import org.openrndr.math.Vector2
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


class TestPlotRegression {

    lateinit var simplePlot: Plot

    lateinit var multilayerPlot: Plot

    lateinit var dotPlot: Plot


    @BeforeTest
    fun setup() {

        simplePlot = Plot(dimensions = Vector2(210.0, 297.0),generator = BasicGrblGenerator())
        simplePlot.draw {
            circle(100.0, 100.0, 30.0)
        }

        multilayerPlot = Plot(dimensions = Vector2(210.0, 297.0),generator = BasicGrblGenerator())
        multilayerPlot.draw {
            lineSegment(0.0, 20.0, 100.0, 200.0)
            lineSegment(0.5, 20.5, 100.5, 200.5)
        }
        multilayerPlot.layer("rect") {
            rectangle(10.1234, 30.1234, 50.1234, 70.1234)
        }
        multilayerPlot.layer("dot") {
            lineSegment(25.55555, 35.55555,25.55555, 35.55555 )
        }
    }


    @Test
    fun `single file with circle`() {


        val expected = """
                G21
                G90
                ;begin layer: default
                ;begin shape
                G0 X70.0 Y100.0
                M3 S255
                G1 X70.28 Y95.885 F500.0
                G1 X71.08 Y91.994 F500.0
                G1 X72.358 Y88.323 F500.0
                G1 X74.103 Y84.846 F500.0
                G1 X76.253 Y81.665 F500.0
                G1 X78.787 Y78.787 F500.0
                G1 X81.665 Y76.253 F500.0
                G1 X84.846 Y74.103 F500.0
                G1 X88.323 Y72.358 F500.0
                G1 X91.994 Y71.08 F500.0
                G1 X95.885 Y70.28 F500.0
                G1 X100.0 Y70.0 F500.0
                G1 X104.115 Y70.28 F500.0
                G1 X108.006 Y71.08 F500.0
                G1 X111.677 Y72.358 F500.0
                G1 X115.154 Y74.103 F500.0
                G1 X118.335 Y76.253 F500.0
                G1 X121.213 Y78.787 F500.0
                G1 X123.747 Y81.665 F500.0
                G1 X125.897 Y84.846 F500.0
                G1 X127.642 Y88.323 F500.0
                G1 X128.92 Y91.994 F500.0
                G1 X129.72 Y95.885 F500.0
                G1 X130.0 Y100.0 F500.0
                G1 X129.72 Y104.115 F500.0
                G1 X128.92 Y108.006 F500.0
                G1 X127.642 Y111.677 F500.0
                G1 X125.897 Y115.154 F500.0
                G1 X123.747 Y118.335 F500.0
                G1 X121.213 Y121.213 F500.0
                G1 X118.335 Y123.747 F500.0
                G1 X115.154 Y125.897 F500.0
                G1 X111.677 Y127.642 F500.0
                G1 X108.006 Y128.92 F500.0
                G1 X104.115 Y129.72 F500.0
                G1 X100.0 Y130.0 F500.0
                G1 X95.885 Y129.72 F500.0
                G1 X91.994 Y128.92 F500.0
                G1 X88.323 Y127.642 F500.0
                G1 X84.846 Y125.897 F500.0
                G1 X81.665 Y123.747 F500.0
                G1 X78.787 Y121.213 F500.0
                G1 X76.253 Y118.335 F500.0
                G1 X74.103 Y115.154 F500.0
                G1 X72.358 Y111.677 F500.0
                G1 X71.08 Y108.006 F500.0
                G1 X70.28 Y104.115 F500.0
                G1 X70.0 Y100.0 F500.0
                M3 S0
                ;end shape
                ;end layer: default
                G0 X0 Y0
                G90
                
            """.trimIndent()

        assertEquals(expected, simplePlot.toCombinedGcode())
    }


    @Test
    fun `multiple files from layers`() {
        val default = """
                G21
                G90
                ;begin layer: default
                ;begin shape
                G0 X0.0 Y20.0
                M3 S255
                G1 X100.0 Y200.0 F500.0
                M3 S0
                ;end shape
                ;begin shape
                G0 X0.5 Y20.5
                M3 S255
                G1 X100.5 Y200.5 F500.0
                M3 S0
                ;end shape
                ;end layer: default
                G0 X0 Y0
                G90
               
                """.trimIndent()
        val rect = """
                G21
                G90
                ;begin layer: rect
                ;begin shape
                G0 X10.123 Y30.123
                M3 S255
                G1 X60.247 Y30.123 F500.0
                G1 X60.247 Y100.247 F500.0
                G1 X10.123 Y100.247 F500.0
                G1 X10.123 Y30.123 F500.0
                M3 S0
                ;end shape
                ;end layer: rect
                G0 X0 Y0
                G90
                
                """.trimIndent()

        val dot = """
                G21
                G90
                ;begin layer: dot
                ;begin shape
                G0 X25.556 Y35.556
                M3 S255
                M3 S0
                ;end shape
                ;end layer: dot
                G0 X0 Y0
                G90
               
                """.trimIndent()

        val got = multilayerPlot.toSplitGcode()
        assertEquals(default, got["default"])
        assertEquals(rect, got["rect"])
        assertEquals(dot, got["dot"])
    }

    @Test
    fun `single file from layers`() {
        val expected = """
                G21
                G90
                ;begin layer: default
                ;begin shape
                G0 X0.0 Y20.0
                M3 S255
                G1 X100.0 Y200.0 F500.0
                M3 S0
                ;end shape
                ;begin shape
                G0 X0.5 Y20.5
                M3 S255
                G1 X100.5 Y200.5 F500.0
                M3 S0
                ;end shape
                ;end layer: default
                ;begin layer: rect
                ;begin shape
                G0 X10.123 Y30.123
                M3 S255
                G1 X60.247 Y30.123 F500.0
                G1 X60.247 Y100.247 F500.0
                G1 X10.123 Y100.247 F500.0
                G1 X10.123 Y30.123 F500.0
                M3 S0
                ;end shape
                ;end layer: rect
                ;begin layer: dot
                ;begin shape
                G0 X25.556 Y35.556
                M3 S255
                M3 S0
                ;end shape
                ;end layer: dot
                G0 X0 Y0
                G90

               """.trimIndent()
        assertEquals(expected, multilayerPlot.toCombinedGcode())
    }

}
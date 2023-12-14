package org.openrndr.extra.jumpfill

import org.openrndr.draw.*
import org.openrndr.extra.fx.blend.Passthrough
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow

@Description("Clustered field")
class ClusteredField(decodeMode: DecodeMode = DecodeMode.DIRECTION,
                     private val outputDistanceToContours: Boolean = true) : Filter1to1() {
    @DoubleParameter("threshold", 0.0, 1.0)
    var threshold = 0.5

    @DoubleParameter("distance scale", 0.0, 1.0)
    var distanceScale = 1.0

    @BooleanParameter("normalized distance")
    var normalizedDistance = false

    @BooleanParameter("unit direction")
    var unitDirection = false

    @BooleanParameter("flip v direction")
    var flipV = true

    private val encodeFilter = EncodePoints()
    private var encoded: ColorBuffer? = null
    private val contourFilter = IdContourPoints()
    private var contoured: ColorBuffer? = null
    private var jumpFlooder: JumpFlooder? = null

    private val decodeFilter = PixelDirection(decodeMode)

    private var fit: ColorBuffer? = null

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(clip == null)
        val advisedWidth = 2.0.pow(ceil(log2(source[0].effectiveWidth.toDouble()))).toInt()
        val advisedHeight = 2.0.pow(ceil(log2(source[0].effectiveHeight.toDouble()))).toInt()
        val advisedSize = max(advisedWidth, advisedHeight)

        fit?.let {
            if (it.effectiveWidth != advisedSize || it.effectiveHeight != advisedSize) {
                it.destroy()
                fit = null
                encoded?.destroy()
                encoded = null
                contoured?.destroy()
                contoured = null
                jumpFlooder?.destroy()
                jumpFlooder = null
            }
        }

        if (fit == null) {
            fit = colorBuffer(advisedSize, advisedSize, type=ColorType.FLOAT32)
        }

        source[0].copyTo(
            fit!!,
            sourceRectangle = IntRectangle(0, 0, source[0].effectiveWidth, source[0].effectiveHeight),
            targetRectangle = IntRectangle(
                0,
                advisedSize - source[0].effectiveHeight,
                source[0].effectiveWidth,
                source[0].effectiveHeight
            )
        )

        if (encoded == null) {
            encoded = colorBuffer(advisedSize, advisedSize, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
        }
        if (jumpFlooder == null) {
            jumpFlooder = JumpFlooder(advisedSize, advisedSize, encodePoints = Passthrough())
        }

        if (outputDistanceToContours && contoured == null) {
            contoured = colorBuffer(advisedSize, advisedSize, type = ColorType.FLOAT32)
        }

        encodeFilter.apply(fit!!, encoded!!)
        var result = jumpFlooder!!.jumpFlood(encoded!!)

        if (outputDistanceToContours) {
            contourFilter.apply(result, contoured!!)
            result = jumpFlooder!!.jumpFlood(contoured!!)
        }

        decodeFilter.outputIds = true
        decodeFilter.originalSize = Vector2(source[0].width.toDouble(), source[0].height.toDouble())
        decodeFilter.distanceScale = distanceScale
        decodeFilter.normalizedDistance = normalizedDistance
        decodeFilter.unitDirection = unitDirection
        decodeFilter.flipV = flipV
        decodeFilter.apply(arrayOf(result, encoded!!), arrayOf(result), clip)

        result.copyTo(
            target[0],
            sourceRectangle = IntRectangle(
                0,
                advisedSize - source[0].effectiveHeight,
                source[0].effectiveWidth,
                source[0].effectiveHeight
            ),
            targetRectangle = IntRectangle(0, 0, source[0].effectiveWidth, source[0].effectiveHeight)
        )
    }

    override fun destroy() {
        encodeFilter.destroy()
        contourFilter.destroy()
        fit?.destroy()
        encoded?.destroy()
        contoured?.destroy()
        jumpFlooder?.destroy()
    }
}
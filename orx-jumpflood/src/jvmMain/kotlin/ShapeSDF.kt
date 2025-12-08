package org.openrndr.extra.jumpfill

import org.openrndr.draw.*
import org.openrndr.extra.jumpflood.jf_shape_sdf
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour


@Description("ShapeSDF")
class ShapeSDF : Filter(filterShaderFromCode(jf_shape_sdf, "shape-sdf")) {
    private val fromBuffer = bufferTexture(1024, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
    private val toBuffer = bufferTexture(1024, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
    private var segmentCount = 0

    @BooleanParameter("use UV map")
    var useUV: Boolean by parameters

    @BooleanParameter("rectify distance")
    var rectify: Boolean by parameters

    private var modelViewMatrixInverse by parameters

    var modelViewMatrix = Matrix44.IDENTITY
        set(value) {
            modelViewMatrixInverse = modelViewMatrix.inversed
            field = value
        }

    init {
        useUV = false
        rectify = false
        modelViewMatrix = Matrix44.IDENTITY
    }

    fun setShapes(shapes: List<Shape>) {
        setContours(shapes.flatMap { it.contours })
    }

    fun setContours(contours: List<ShapeContour>) {
        val from = mutableListOf<Vector4>()
        val to = mutableListOf<Vector4>()

        for (contour in contours) {
            val lin = contour.sampleLinear()
            var contourLength = 0.0
            for (segment in lin.segments) {
                contourLength += segment.length
            }
            var offset = 0.0
            for (segment in lin.segments) {
                from.add(Vector4(segment.start.x, segment.start.y, offset, contourLength))
                offset += segment.length
                to.add(Vector4(segment.end.x, segment.end.y, offset, contourLength))
            }
        }

        val fromShadow = fromBuffer.shadow
        val fromWriter = fromShadow.writer()
        fromWriter.rewind()
        for (v in from) {
            fromWriter.write(v)
        }
        fromShadow.upload(0, from.size * 4 * 4)

        val toShadow = toBuffer.shadow
        val toWriter = toShadow.writer()
        toWriter.rewind()
        for (v in to) {
            toWriter.write(v)
        }
        toShadow.upload(0, to.size * 4 * 4)

        segmentCount = from.size
    }

    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>, clip: Rectangle?) {
        require(target[0].type == ColorType.FLOAT16 || target[0].type == ColorType.FLOAT32) {
            "needs a floating point target"
        }
        parameters["fromBuffer"] = fromBuffer
        parameters["toBuffer"] = toBuffer
        parameters["segmentCount"] = segmentCount
        // -- bit of an hack
        val effectiveSource = if (source.isNotEmpty()) source else target
        super.apply(effectiveSource, target, clip)
    }
}
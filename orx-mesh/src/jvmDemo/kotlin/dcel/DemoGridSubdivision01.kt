package dcel

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolated
import org.openrndr.extra.color.spaces.OKHSV
import org.openrndr.extra.color.tools.shiftHue
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.mesh.dcel.convert.toDcel
import org.openrndr.extra.mesh.dcel.modify.convexFaceSetSubdivide
import org.openrndr.extra.mesh.dcel.modify.edgeInsert
import org.openrndr.extra.mesh.dcel.modify.edgeRemove
import org.openrndr.extra.mesh.dcel.modify.vertexChamfer
import org.openrndr.extra.mesh.dcel.query.edgeForFaces
import org.openrndr.extra.mesh.dcel.query.edgeLoopIndices
import org.openrndr.extra.mesh.dcel.query.verticesForFace
import org.openrndr.extra.mesh.generate.gridMesh
import org.openrndr.extra.noise.uniform
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {

            val grid = gridMesh(drawer.bounds, 10, 10)
            val dcel = grid.toDcel()

//            dcel.vertices.forEach {
//                it.position = it.position + Vector2.uniformRing(5.0, 10.0).xy0
//            }
//


            dcel.convexFaceSetSubdivide(setOf(14, 15, 45,46))
            dcel.convexFaceSetSubdivide(setOf(35, 36))
            val r = dcel.convexFaceSetSubdivide(setOf(65))
            dcel.convexFaceSetSubdivide(r)
            dcel.convexFaceSetSubdivide(setOf(66))
            dcel.convexFaceSetSubdivide(setOf(55))
            val e = dcel.edgeForFaces(85, 86)
            dcel.edgeRemove(dcel.halfEdges[e])

            dcel.apply {
                var v = verticesForFace(42).first()
                vertexChamfer(v, 10.0)
            }


            val shapes = (0 until dcel.faces.size).map { dcel.faceToShape(it) }





            extend {
                for ((index, shape) in shapes.withIndex()) {
                    drawer.fill = ColorRGBa.PINK.shiftHue<OKHSV>(index * 45.0)
                    drawer.stroke = ColorRGBa.BLACK
                    drawer.shape(shape)
                }

            }
        }
    }
}
package dcel

import org.openrndr.application
import org.openrndr.draw.isolated
import org.openrndr.extra.mesh.dcel.convert.faceToShape
import org.openrndr.extra.mesh.dcel.convert.shapeToDcelNoTriangulation
import org.openrndr.extra.shapes.primitives.grid
import org.openrndr.shape.Circle
import org.openrndr.shape.Shape

fun main() {
    application {
        configure {
            width = 720
            height = 720
        }
        program {
            val grid = drawer.bounds.grid(2,2).flatten()

            val shape = Circle(grid[0].center, 100.0).shape
            val shapeHole =
                Shape(
                    listOf(
                        Circle(grid[2].center, 100.0).contour,
                        Circle(grid[2].center, 50.0).contour.reversed)
                )


            val dcel = shapeToDcelNoTriangulation(shape, 10.0)
            val dcelHole = shapeToDcelNoTriangulation(shapeHole, 10.0)

            val shapeR = dcel.faceToShape(0)
            val shapeHoleR = dcelHole.faceToShape(0)
            extend {
                drawer.shape(shape)
                drawer.isolated {
                    drawer.translate(grid[1].corner)
                    drawer.shape(shapeR)
                }
                drawer.shape(shapeHole)
                drawer.translate(width / 2.0, 0.0)
                drawer.shape(shapeHoleR)
            }
        }
    }
}
//import micycle.pgs.PGS_Voronoi
//import org.openrndr.application
//import org.openrndr.extra.processing.toPVector
//import org.openrndr.extra.processing.toShape
//import org.openrndr.math.Vector3
//import kotlin.random.Random
//
///**
// * Requires the Processing Geometry Suite (PGS) enabled in build.gradle.kts.
// *
// * Demonstrates the use of `PGS_Voronoi.multiplicativelyWeightedVoronoi`.
// * The program creates a weighted Voronoi diagram using a collection of
// * random points. The `z` component in each `Vector3` point defines
// * its weight, affecting the curvature of the edges the resulting shapes.
// *
// */
//fun main() = application {
//    program {
//        val points = List(80) {
//            Vector3(
//                Random.nextDouble() * width,
//                Random.nextDouble() * height,
//                Random.nextDouble() * 0.2 + 0.5
//            ).toPVector()
//        }
//
//        val bounds = arrayOf(0.0, 0.0, width * 1.0, height * 1.0).toDoubleArray()
//
//        val design = PGS_Voronoi.multiplicativelyWeightedVoronoi(points, bounds, true)
//
//        val rs = design.toShape()
//        extend {
//            drawer.shape(rs)
//        }
//    }
//}
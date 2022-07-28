package org.openrndr.extra.computegraph.nodes

import org.openrndr.Program
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.computegraph.ComputeGraph
import org.openrndr.extra.computegraph.ComputeNode
import org.openrndr.extra.computegraph.withKey
import org.openrndr.extra.imageFit.imageFit

fun ComputeGraph.fitImageNode(program: Program, input: ComputeNode) : ComputeNode {
    return node {
        name = "fit-image"
        val rt = renderTarget(program.width, program.height) {
            colorBuffer()
        }
        val inputImage: ColorBuffer by input.outputs.withKey("image")
        var outputImage:ColorBuffer by outputs.withKey("image")
        outputImage = rt.colorBuffer(0)
        compute {
            program.drawer.isolatedWithTarget(rt) {
                ortho(rt)
                imageFit(inputImage, bounds)
            }
        }
        dependOn(input)
    }
}
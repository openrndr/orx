package org.openrndr.extra.computegraph.nodes

import org.openrndr.Program
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.computegraph.ComputeGraph
import org.openrndr.extra.computegraph.ComputeNode
import java.io.File

fun ComputeGraph.dropImageNode(program: Program): ComputeNode {
    return node {
        name = "drop-image"
        var file: File by inputs
        file = File("data/images/cheeta.jpg")
        program.window.drop.listen {
            file = File(it.files.first())
        }
        var image: ColorBuffer by outputs

        fun loadFileOrEmpty() = if (file.exists()) loadImage(file) else colorBuffer(256, 256)
        image = loadFileOrEmpty()
        compute {
            image.destroy()
            image = loadFileOrEmpty()
        }
    }
}
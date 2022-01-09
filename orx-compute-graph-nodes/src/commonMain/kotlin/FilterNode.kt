package org.openrndr.extra.computegraph.nodes

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Filter
import org.openrndr.draw.createEquivalent
import org.openrndr.extra.computegraph.ComputeGraph
import org.openrndr.extra.computegraph.ComputeNode
import org.openrndr.extra.computegraph.withKey

fun <T : Filter> ComputeGraph.filterNode(
    filter: T, input: ComputeNode, inputKey: String = "image", outputKey: String = "image",
    config: ComputeNode.(f: Filter) -> Unit
): ComputeNode {
    return node {
        name = "filter-${filter::class.simpleName}"
        inputs = filter.parameters
        config(filter)
        val inputImage by input.outputs.withKey<ColorBuffer>(inputKey)
        var outputImage by outputs.withKey<ColorBuffer>(outputKey)
        outputImage = inputImage.createEquivalent()
        compute {
            filter.apply(inputImage, outputImage)
        }
        dependOn(input)

    }
}
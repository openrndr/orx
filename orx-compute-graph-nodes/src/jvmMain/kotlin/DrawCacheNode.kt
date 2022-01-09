package org.openrndr.extra.computegraph.nodes

import mu.KotlinLogging
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.RequestAssetsEvent
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.computegraph.ComputeGraph
import org.openrndr.extra.computegraph.ComputeNode
import org.openrndr.extra.computegraph.withKey
import java.io.File

val logger = KotlinLogging.logger { }

private data class RenderTargetDescription(val width: Int, val height: Int, val contentScale: Double)

private fun RenderTarget.description() = RenderTargetDescription(width, height, contentScale)


fun ComputeGraph.drawCacheNode(
    program: Program,
    inputNodes: List<ComputeNode>,
    draw: Program.(node: ComputeNode) -> Unit
): ComputeNode {
    return node {
        var producingAssets: Boolean by inputs
        producingAssets = false

        program.keyboard.keyDown.listen {
            if (!it.propagationCancelled) {
                logger.info { "requesting assets" }
                if (it.key == KEY_SPACEBAR) {
                    program.requestAssets.trigger(RequestAssetsEvent(this, program))
                }
            }
        }

        var screenshotTarget = ""
        program.produceAssets.listen {
            producingAssets = true
            screenshotTarget = "screenshots/${it.assetMetadata.assetBaseName}.png"
        }

        name = "draw-cache"
        var rt = renderTarget(program.width, program.height) {
            colorBuffer()
            depthBuffer()
        }
        var outputImage: ColorBuffer by outputs.withKey("image")
        outputImage = rt.colorBuffer(0)

        var description: RenderTargetDescription by inputs
        description = RenderTarget.active.description()

        update {
            description = RenderTarget.active.description()
        }
        val defaultContentScale = program.window.contentScale

        compute {
            rt.colorBuffer(0).destroy()
            rt.depthBuffer?.destroy()
            rt.detachColorAttachments()
            rt.detachDepthBuffer()
            rt.destroy()
            rt = renderTarget(
                program.width,
                program.height,
                contentScale = if (producingAssets) 6.0 else defaultContentScale
            ) {
                colorBuffer()
                depthBuffer()
            }
            program.drawer.isolatedWithTarget(rt) {
                clear(ColorRGBa.WHITE)

                draw(program, this@node)
            }
            outputImage = rt.colorBuffer(0)
            println(outputImage)

            if (producingAssets) {
                logger.info { "saving draw cache to file" }
                val directory = File("screenshots")
                if (!directory.exists()) {
                    directory.mkdirs()
                }

                outputImage.saveToFile(File(screenshotTarget), async = false)
                producingAssets = false
                screenshotTarget = ""
            }
        }
        for (input in inputNodes) {
            dependOn(input)
        }
    }
}
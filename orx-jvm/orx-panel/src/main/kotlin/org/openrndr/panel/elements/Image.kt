package org.openrndr.panel.elements

import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.imageFit.imageFit


class Image : Element(ElementType("image")) {
    private val dummyImage = colorBuffer(32, 32)
    var image = dummyImage
        set(value) {
            if (field != value) {
                field = value
                requestRedraw()
            }
        }

    override val widthHint: Double
        get() = image.width.toDouble()

    override val heightHint: Double
        get() = image.height.toDouble()

    override fun draw(drawer: Drawer) {
        drawer.imageFit(image, layout.contentBoundsAtOrigin)
    }

    var ownsImage = false
    override fun close() {
        super.close()
        dummyImage.close()
        if (ownsImage) {
            image.close()
        }
    }
}
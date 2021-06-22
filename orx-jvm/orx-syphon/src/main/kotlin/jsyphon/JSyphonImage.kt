package org.openrndr.extra.syphon.jsyphon

class JSyphonImage(private val name: Int, private val width: Int, private val height: Int) {
    fun textureName(): Int {
        return name
    }

    fun textureWidth(): Int {
        return width
    }

    fun textureHeight(): Int {
        return height
    }
}
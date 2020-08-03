package org.openrndr.boofcv.binding

import boofcv.abst.distort.FDistort
import boofcv.struct.image.ImageBase

fun <T : ImageBase<out ImageBase<*>>?> ImageBase<T>.resize(scaleX: Double, scaleY: Double = scaleX): T {
    val scaled = this.createNew((this.width * scaleX).toInt(), (this.height * scaleY).toInt())

    FDistort(this, scaled).scaleExt().apply()

    return scaled
}
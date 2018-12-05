package org.openrndr.extra.noise


fun Double.fastFloor(): Int {
    return if (this >= 0) this.toInt() else this.toInt() - 1
}
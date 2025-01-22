package org.openrndr.extra.shapes.ordering

fun mortonToHilbert2d(morton: UInt, bits: Int): UInt {
    var hilbert = 0u
    var remap = 0xb4u
    var block = (bits shl 1)
    while (block != 0) {
        block -= 2
        var mcode = ((morton shr block) and 3u)
        var hcode = ((remap shr (mcode shl 1).toInt()) and 3u)
        remap = remap xor (0x82000028u shr (hcode shl 3).toInt())
        hilbert = ((hilbert shl 2) + hcode)
    }
    return (hilbert);
}

fun hilbertToMorton2d(hilbert: UInt, bits: Int): UInt {
    var morton = 0u
    var remap = 0xb4u
    var block = (bits shl 1)
    while (block != 0) {
        block -= 2;
        var hcode = ((hilbert shr block) and 3u)
        var mcode = ((remap shr (hcode shl 1).toInt()) and 3u)
        remap = remap xor (0x330000ccu shr (hcode shl 3).toInt())
        morton = ((morton shl 2) + mcode)
    }
    return (morton)
}
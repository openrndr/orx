package org.openrndr.extra.shapes.ordering

@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert2dDecode16Bit(hilbert: UInt): UIntArray {
    val morton = hilbertToMorton2d(hilbert, 10)
    return morton2dDecode16Bit(morton)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert2dEncode16Bit(index1: UInt, index2: UInt): UInt {
    val morton = morton2dEncode16Bit(index1, index2)
    return mortonToHilbert2d(morton, 16)
}


@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert2dDecode5Bit(hilbert: UInt): UIntArray {
    val morton = hilbertToMorton2d(hilbert, 5)
    return morton2dDecode5Bit(morton)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert2dEncode5Bit(index1: UInt, index2: UInt): UInt {
    val morton = morton2dEncode5Bit(index1, index2)
    return mortonToHilbert3d(morton, 5)
}

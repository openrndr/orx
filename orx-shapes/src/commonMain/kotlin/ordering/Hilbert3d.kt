package org.openrndr.extra.shapes.ordering

@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert3dDecode10Bit(hilbert: UInt): UIntArray {
    val morton = hilbertToMorton3d(hilbert, 10)
    return morton3dDecode10Bit(morton)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert3dEncode10Bit(index1: UInt, index2: UInt, index3: UInt): UInt {
    val morton = morton3dEncode10Bit(index1, index2, index3)
    return mortonToHilbert3d(morton, 10)
}


@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert3dDecode5Bit(hilbert: UInt): UIntArray {
    val morton = hilbertToMorton3d(hilbert, 5)
    return morton3dDecode5Bit(morton)
}

@OptIn(ExperimentalUnsignedTypes::class)
fun hilbert3dEncode5Bit(index1: UInt, index2: UInt, index3: UInt): UInt {
    val morton = morton3dEncode5Bit(index1, index2, index3)
    return mortonToHilbert3d(morton, 5)
}

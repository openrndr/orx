@file:OptIn(ExperimentalUnsignedTypes::class)

package org.openrndr.extra.shapes.ordering

fun morton2dEncode5Bit(index1: UInt, index2: UInt): UInt {
    // pack 2 5-bit indices into a 10-bit Morton code
    var index1: UInt = index1
    var index2: UInt = index2
    index1 = index1 and 0x0000001fu
    index2 = index2 and 0x0000001fu
    index1 *= 0x01041041u
    index2 *= 0x01041041u
    index1 = index1 and 0x10204081u
    index2 = index2 and 0x10204081u
    index1 *= 0x00108421u
    index2 *= 0x00108421u
    index1 = index1 and 0x15500000u
    index2 = index2 and 0x15500000u
    return ((index1 shr 20) or (index2 shr 19))
}

fun morton2dDecode5Bit(morton: UInt): UIntArray { // unpack 2 5-bit indices from a 10-bit Morton code
    var value1 = morton;
    var value2 = (value1 shr 1)
    value1 = value1 and 0x00000155u
    value2 = value2 and 0x00000155u
    value1 = value1 or (value1 shr 1)
    value2 = value2 or (value2 shr 1)
    value1 = value1 and 0x00000133u
    value2 = value2 and 0x00000133u
    value1 = value1 or (value1 shr 2)
    value2 = value2 or (value2 shr 2)
    value1 = value1 and 0x0000010fu
    value2 = value2 and 0x0000010fu
    value1 = value1 or (value1 shr 4)
    value2 = value2 or (value2 shr 4)
    value1 = value1 and 0x0000001fu
    value2 = value2 and 0x0000001fu
    return uintArrayOf(value1, value2)
}

fun morton2dEncode16Bit(index1: UInt, index2: UInt): UInt { // pack 2 16-bit indices into a 32-bit Morton code
    var index1: UInt = index1
    var index2: UInt = index2
    index1 = index1 and 0x0000ffffu
    index2 = index2 and 0x0000ffffu
    index1 = index1 or (index1 shl 8)
    index2 = index2 or (index2 shl 8)
    index1 = index1 and 0x00ff00ffu
    index2 = index2 and 0x00ff00ffu
    index1 = index1 or (index1 shl 4)
    index2 = index2 or (index2 shl 4)
    index1 = index1 and 0x0f0f0f0fu
    index2 = index2 and 0x0f0f0f0fu
    index1 = index1 or (index1 shl 2)
    index2 = index2 or (index2 shl 2)
    index1 = index1 and 0x33333333u
    index2 = index2 and 0x33333333u
    index1 = index1 or (index1 shl 1)
    index2 = index2 or (index2 shl 1)
    index1 = index1 and 0x55555555u
    index2 = index2 and 0x55555555u
    return (index1 or (index2 shl 1))
}

fun morton2dDecode16Bit(morton: UInt): UIntArray { // unpack 2 16-bit indices from a 32-bit Morton code
    var value1 = morton;
    var value2 = (value1 shr 1);
    value1 = value1 and 0x55555555u
    value2 = value2 and 0x55555555u
    value1 = value1 or (value1 shr 1)
    value2 = value2 or (value2 shr 1)
    value1 = value1 and 0x33333333u
    value2 = value2 and 0x33333333u
    value1 = value1 or (value1 shr 2)
    value2 = value2 or (value2 shr 2)
    value1 = value1 and 0x0f0f0f0fu
    value2 = value2 and 0x0f0f0f0fu
    value1 = value1 or (value1 shr 4)
    value2 = value2 or (value2 shr 4)
    value1 = value1 and 0x00ff00ffu
    value2 = value2 and 0x00ff00ffu
    value1 = value1 or (value1 shr 8)
    value2 = value2 or (value2 shr 8)
    value1 = value1 and 0x0000ffffu
    value2 = value2 and 0x0000ffffu
    return uintArrayOf(value1, value2)
}
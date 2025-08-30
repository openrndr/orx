package org.openrndr.extra.shapes.ordering
/*
https://and-what-happened.blogspot.com/2011/08/fast-2d-and-3d-hilbert-curves-and.html
 */


fun morton3dEncode5Bit(
    index1: UInt,
    index2: UInt,
    index3: UInt
): UInt { // pack 3 5-bit indices into a 15-bit Morton code
    var index1 = index1
    var index2 = index2
    var index3 = index3


    index1 = index1 and 0x0000001fU
    index2 = index2 and 0x0000001fU
    index3 = index3 and 0x0000001fU
    index1 *= 0x01041041U
    index2 *= 0x01041041U
    index3 *= 0x01041041U
    index1 = index1 and 0x10204081U
    index2 = index2 and 0x10204081U
    index3 = index3 and 0x10204081U
    index1 *= 0x00011111U
    index2 *= 0x00011111U
    index3 *= 0x00011111U
    index1 = index1 and 0x12490000U
    index2 = index2 and 0x12490000U
    index3 = index3 and 0x12490000U
    return ((index1 shr 16) or (index2 shr 15) or (index3 shr 14));
}

@OptIn(ExperimentalUnsignedTypes::class)
fun morton3dDecode5Bit(morton: UInt): UIntArray { // unpack 3 5-bit indices from a 15-bit Morton code
    var value1 = morton;
    var value2 = (value1 shr 1);
    var value3 = (value1 shr 2);
    value1 = value1 and 0x00001249U
    value2 = value2 and 0x00001249U
    value3 = value3 and 0x00001249U
    value1 = value1 or (value1 shr 2);
    value2 = value2 or (value2 shr 2);
    value3 = value3 or (value3 shr 2);
    value1 = value1 and 0x000010c3U
    value2 = value2 and 0x000010c3U
    value3 = value3 and 0x000010c3U
    value1 = value1 or (value1 shr 4);
    value2 = value2 or (value2 shr 4);
    value3 = value3 or (value3 shr 4);
    value1 = value1 and 0x0000100fU
    value2 = value2 and 0x0000100fU
    value3 = value3 and 0x0000100fU
    value1 = value1 or (value1 shr 8);
    value2 = value2 or (value2 shr 8);
    value3 = value3 or (value3 shr 8);
    value1 = value1 and 0x0000001fU
    value2 = value2 and 0x0000001fU
    value3 = value3 and 0x0000001fU
    return uintArrayOf(value1, value2, value3)
}


@OptIn(ExperimentalUnsignedTypes::class)
fun morton3dEncode10Bit(
    index1: UInt,
    index2: UInt,
    index3: UInt
): UInt { // pack 3 10-bit indices into a 30-bit Morton code

    var index1 = index1
    var index2 = index2
    var index3 = index3

    index1 = index1 and 0x000003ffU
    index2 = index2 and 0x000003ffU
    index3 = index3 and 0x000003ffU
    index1 = index1 or (index1 shl 16)
    index2 = index2 or (index2 shl 16)
    index3 = index3 or (index3 shl 16)
    index1 = index1 and 0x030000ffU
    index2 = index2 and 0x030000ffU
    index3 = index3 and 0x030000ffU
    index1 = index1 or (index1 shl 8)
    index2 = index2 or (index2 shl 8)
    index3 = index3 or (index3 shl 8)
    index1 = index1 and 0x0300f00fU
    index2 = index2 and 0x0300f00fU
    index3 = index3 and 0x0300f00fU
    index1 = index1 or (index1 shl 4)
    index2 = index2 or (index2 shl 4)
    index3 = index3 or (index3 shl 4)
    index1 = index1 and 0x030c30c3U
    index2 = index2 and 0x030c30c3U
    index3 = index3 and 0x030c30c3U
    index1 = index1 or (index1 shl 2)
    index2 = index2 or (index2 shl 2)
    index3 = index3 or (index3 shl 2)
    index1 = index1 and 0x09249249U
    index2 = index2 and 0x09249249U
    index3 = index3 and 0x09249249U
    return (index1 or (index2 shl 1) or (index3 shl 2))
}

@OptIn(ExperimentalUnsignedTypes::class)
fun morton3dDecode10Bit(morton: UInt): UIntArray { // unpack 3 10-bit indices from a 30-bit Morton code
    var value1 = morton
    var value2 = (value1 shr 1)
    var value3 = (value1 shr 2)
    value1 = value1 and 0x09249249U
    value2 = value2 and 0x09249249U
    value3 = value3 and 0x09249249U
    value1 = value1 or (value1 shr 2)
    value2 = value2 or (value2 shr 2)
    value3 = value3 or (value3 shr 2)
    value1 = value1 and 0x030c30c3U
    value2 = value2 and 0x030c30c3U
    value3 = value3 and 0x030c30c3U
    value1 = value1 or (value1 shr 4)
    value2 = value2 or (value2 shr 4)
    value3 = value3 or (value3 shr 4)
    value1 = value1 and 0x0300f00fU
    value2 = value2 and 0x0300f00fU
    value3 = value3 and 0x0300f00fU
    value1 = value1 or (value1 shr 8)
    value2 = value2 or (value2 shr 8)
    value3 = value3 or (value3 shr 8)
    value1 = value1 and 0x030000ffU
    value2 = value2 and 0x030000ffU
    value3 = value3 and 0x030000ffU
    value1 = value1 or (value1 shr 16)
    value2 = value2 or (value2 shr 16)
    value3 = value3 or (value3 shr 16)
    value1 = value1 and 0x000003ffU;
    value2 = value2 and 0x000003ffU;
    value3 = value3 and 0x000003ffU;
    return uintArrayOf(value1, value2, value3)
}



@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    val decoded = morton3dDecode10Bit(300U)
    val encoded = morton3dEncode10Bit(decoded[0], decoded[1], decoded[2])
    println(decoded)
    println(encoded)

    run {
        val decoded = hilbert3dDecode10Bit(300U)
        val encoded = hilbert3dEncode10Bit(decoded[0], decoded[1], decoded[2])
        println(decoded)
        println(encoded)
    }
}
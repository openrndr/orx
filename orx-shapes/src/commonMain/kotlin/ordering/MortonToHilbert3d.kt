package org.openrndr.extra.shapes.ordering

/* https://threadlocalmutex.com/?p=149 */
@OptIn(ExperimentalUnsignedTypes::class)
private val mortonToHilbertTable = uintArrayOf(
    48u, 33u, 27u, 34u, 47u, 78u, 28u, 77u,
    66u, 29u, 51u, 52u, 65u, 30u, 72u, 63u,
    76u, 95u, 75u, 24u, 53u, 54u, 82u, 81u,
    18u, 3u, 17u, 80u, 61u, 4u, 62u, 15u,
    0u, 59u, 71u, 60u, 49u, 50u, 86u, 85u,
    84u, 83u, 5u, 90u, 79u, 56u, 6u, 89u,
    32u, 23u, 1u, 94u, 11u, 12u, 2u, 93u,
    42u, 41u, 13u, 14u, 35u, 88u, 36u, 31u,
    92u, 37u, 87u, 38u, 91u, 74u, 8u, 73u,
    46u, 45u, 9u, 10u, 7u, 20u, 64u, 19u,
    70u, 25u, 39u, 16u, 69u, 26u, 44u, 43u,
    22u, 55u, 21u, 68u, 57u, 40u, 58u, 67u
)

@OptIn(ExperimentalUnsignedTypes::class)
private val hilbertToMortonTable = uintArrayOf(
    48u, 33u, 35u, 26u, 30u, 79u, 77u, 44u,
    78u, 68u, 64u, 50u, 51u, 25u, 29u, 63u,
    27u, 87u, 86u, 74u, 72u, 52u, 53u, 89u,
    83u, 18u, 16u, 1u, 5u, 60u, 62u, 15u,
    0u, 52u, 53u, 57u, 59u, 87u, 86u, 66u,
    61u, 95u, 91u, 81u, 80u, 2u, 6u, 76u,
    32u, 2u, 6u, 12u, 13u, 95u, 91u, 17u,
    93u, 41u, 40u, 36u, 38u, 10u, 11u, 31u,
    14u, 79u, 77u, 92u, 88u, 33u, 35u, 82u,
    70u, 10u, 11u, 23u, 21u, 41u, 40u, 4u,
    19u, 25u, 29u, 47u, 46u, 68u, 64u, 34u,
    45u, 60u, 62u, 71u, 67u, 18u, 16u, 49u
)

@OptIn(ExperimentalUnsignedTypes::class)
private fun transformCurve(input: UInt, bits: Int, lookupTable: UIntArray): UInt {
    var transform = 0u
    var out = 0u
    for (i in 3 * (bits - 1) downTo 0 step 3) {
        transform = lookupTable[(transform or ((input shr i) and 7U)).toInt()];
        out = (out shl 3) or (transform and 7U)
        transform = transform and (7U).inv()
    }
    return out;
}

@OptIn(ExperimentalUnsignedTypes::class)
fun mortonToHilbert3d(mortonIndex: UInt, bits: Int = 10): UInt {
    return transformCurve(mortonIndex, bits, mortonToHilbertTable)
}


@OptIn(ExperimentalUnsignedTypes::class)
fun hilbertToMorton3d(hilbertIndex: UInt, bits: Int = 10): UInt {
    return transformCurve(hilbertIndex, bits, hilbertToMortonTable)
}

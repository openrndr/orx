package org.openrndr.extra.tensorflow.arrays

typealias FloatArray2D = Array<FloatArray>
typealias FloatArray3D = Array<Array<FloatArray>>
typealias FloatArray4D = Array<Array<Array<FloatArray>>>
typealias FloatArray5D = Array<Array<Array<Array<FloatArray>>>>
typealias FloatArray6D = Array<Array<Array<Array<Array<FloatArray>>>>>

typealias IntArray2D = Array<IntArray>
typealias IntArray3D = Array<Array<IntArray>>
typealias IntArray4D = Array<Array<Array<IntArray>>>
typealias IntArray5D = Array<Array<Array<Array<IntArray>>>>
typealias IntArray6D = Array<Array<Array<Array<Array<IntArray>>>>>

typealias BooleanArray2D = Array<BooleanArray>
typealias BooleanArray3D = Array<Array<BooleanArray>>
typealias BooleanArray4D = Array<Array<Array<BooleanArray>>>
typealias BooleanArray5D = Array<Array<Array<Array<BooleanArray>>>>
typealias BooleanArray6D = Array<Array<Array<Array<Array<BooleanArray>>>>>

typealias LongArray2D = Array<LongArray>
typealias LongArray3D = Array<Array<LongArray>>
typealias LongArray4D = Array<Array<Array<LongArray>>>
typealias LongArray5D = Array<Array<Array<Array<LongArray>>>>
typealias LongArray6D = Array<Array<Array<Array<Array<LongArray>>>>>

typealias ByteArray2D = Array<ByteArray>
typealias ByteArray3D = Array<Array<ByteArray>>
typealias ByteArray4D = Array<Array<Array<ByteArray>>>
typealias ByteArray5D = Array<Array<Array<Array<ByteArray>>>>
typealias ByteArray6D = Array<Array<Array<Array<Array<ByteArray>>>>>

typealias DoubleArray2D = Array<DoubleArray>
typealias DoubleArray3D = Array<Array<DoubleArray>>
typealias DoubleArray4D = Array<Array<Array<DoubleArray>>>
typealias DoubleArray5D = Array<Array<Array<Array<DoubleArray>>>>
typealias DoubleArray6D = Array<Array<Array<Array<Array<DoubleArray>>>>>

fun floatArray2D(y: Int, x: Int): FloatArray2D = Array(y) { FloatArray(x) }
fun floatArray3D(z: Int, y: Int, x: Int): FloatArray3D = Array(z) { Array(y) { FloatArray(x) } }
fun floatArray4D(w: Int, z: Int, y: Int, x: Int): FloatArray4D = Array(w) { Array(z) { Array(y) { FloatArray(x) } } }

fun doubleArray2D(y: Int, x: Int): DoubleArray2D = Array(y) { DoubleArray(x) }
fun doubleArray3D(z: Int, y: Int, x: Int): DoubleArray3D = Array(z) { Array(y) { DoubleArray(x) } }
fun doubleArray4D(w: Int, z: Int, y: Int, x: Int): DoubleArray4D = Array(w) { Array(z) { Array(y) { DoubleArray(x) } } }

fun intArray2D(y: Int, x: Int): IntArray2D = Array(y) { IntArray(x) }
fun intArray3D(z: Int, y: Int, x: Int): IntArray3D = Array(z) { Array(y) { IntArray(x) } }
fun intArray4D(w: Int, z: Int, y: Int, x: Int): IntArray4D = Array(w) { Array(z) { Array(y) { IntArray(x) } } }

fun longArray2D(y: Int, x: Int): LongArray2D = Array(y) { LongArray(x) }
fun longArray3D(z: Int, y: Int, x: Int): LongArray3D = Array(z) { Array(y) { LongArray(x) } }
fun longArray4D(w: Int, z: Int, y: Int, x: Int): LongArray4D = Array(w) { Array(z) { Array(y) { LongArray(x) } } }

fun byteArray2D(y: Int, x: Int): ByteArray2D = Array(y) { ByteArray(x) }
fun byteArray3D(z: Int, y: Int, x: Int): ByteArray3D = Array(z) { Array(y) { ByteArray(x) } }
fun byteArray4D(w: Int, z: Int, y: Int, x: Int): ByteArray4D = Array(w) { Array(z) { Array(y) { ByteArray(x) } } }

fun booleanArray2D(y: Int, x: Int): BooleanArray2D = Array(y) { BooleanArray(x) }
fun booleanArray3D(z: Int, y: Int, x: Int): BooleanArray3D = Array(z) { Array(y) { BooleanArray(x) } }
fun booleanArray4D(w: Int, z: Int, y: Int, x: Int): BooleanArray4D = Array(w) { Array(z) { Array(y) { BooleanArray(x) } } }

operator fun FloatArray2D.get(y: Int, x: Int) = this[y][x]
operator fun FloatArray3D.get(z: Int, y: Int, x: Int) = this[z][y][x]
operator fun FloatArray4D.get(w: Int, z: Int, y: Int, x: Int) = this[w][z][y][x]

operator fun DoubleArray2D.get(y: Int, x: Int) = this[y][x]
operator fun DoubleArray3D.get(z: Int, y: Int, x: Int) = this[z][y][x]
operator fun DoubleArray4D.get(w: Int, z: Int, y: Int, x: Int) = this[w][z][y][x]

operator fun IntArray2D.get(y: Int, x: Int) = this[y][x]
operator fun IntArray3D.get(z: Int, y: Int, x: Int) = this[z][y][x]
operator fun IntArray4D.get(w: Int, z: Int, y: Int, x: Int) = this[w][z][y][x]

operator fun LongArray2D.get(y: Int, x: Int) = this[y][x]
operator fun LongArray3D.get(z: Int, y: Int, x: Int) = this[z][y][x]
operator fun LongArray4D.get(w: Int, z: Int, y: Int, x: Int) = this[w][z][y][x]

operator fun ByteArray2D.get(y: Int, x: Int) = this[y][x]
operator fun ByteArray3D.get(z: Int, y: Int, x: Int) = this[z][y][x]
operator fun ByteArray4D.get(w: Int, z: Int, y: Int, x: Int) = this[w][z][y][x]



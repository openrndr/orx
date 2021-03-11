package org.openrndr.extra.noise

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import org.openrndr.extra.noise.fbm as orxFbm
import kotlin.random.Random as DefaultRandom

private data class RandomState(var seed: String, var rng: DefaultRandom)
/**
 * Deterministic Random using a seed to guarantee the same random values between iterations
 */
object Random {
    var rnd: DefaultRandom

    private var seedTracking: Int = 0
    private var nextGaussian: Double = 0.0
    private var hasNextGaussian = false

    private lateinit var state: RandomState

    enum class Fractal {
        FBM, BILLOW, RIGID
    }

    enum class Noise {
        LINEAR, QUINTIC, HERMIT
    }

    var seed: String = "OPENRNDR"
        set(value) {
            field = value
            rnd = newRandomGenerator(value)
        }

    init {
        rnd = newRandomGenerator(seed)

        state = RandomState(seed, rnd)
    }

    private fun newRandomGenerator(newSeed: String): DefaultRandom {
        return DefaultRandom(stringToInt(newSeed))
    }

    private fun stringToInt(str: String): Int = str.toCharArray().fold(0) { i: Int, c: Char ->
        i + c.toInt()
    }

    fun resetState() {
        rnd = newRandomGenerator(seed)
    }

    fun randomizeSeed() {
        val seedBase = seed.replace(Regex("""-\d+"""), "")
        seedTracking = int0(999999)
        seed = "${seedBase}-${seedTracking}"
    }

    /**
     * Use this when you want to get non-deterministic values aka random every call
     *
     * @param fn
     */
    fun unseeded(fn: () -> Unit) {
        push()

        fn()

        pop()
    }

    /**
     * Use this when you want to get non-deterministic values aka random every call
     * Follow it by calling .pop
     */
    fun push() {
        state = RandomState(seed, rnd)

        rnd = DefaultRandom
    }

    /**
     * Use this to go back to seeded state
     */
    fun pop() {
        resetState()

        seed = state.seed
        rnd = state.rng
    }

    /**
     * Use this inside `extend` methods to get the same result between iterations
     *
     * @param fn
     */
    fun isolated(fn: Random.() -> Unit) {
        val state = rnd
        val currentSeed = seed

        resetState()

        this.fn()

        seed = currentSeed
        rnd = state
    }

    fun double(min: Double = -1.0, max: Double = 1.0): Double {
        return Double.uniform(min, max, rnd)
    }

    fun double0(max: Double = 1.0): Double {
        return rnd.nextDouble(max)
    }

    fun int0(max: Int = Int.MAX_VALUE): Int {
        return rnd.nextInt(max)
    }

    fun int(min: Int = 0, max: Int = Int.MAX_VALUE): Int {
        return rnd.nextInt(min, max)
    }

    fun bool(probability: Double = 0.5): Boolean {
        return rnd.nextDouble(1.0) < probability
    }

    fun <T> pick(coll: Collection<T>): T {
        return pick(coll, count = 1).first()
    }

    fun <T> pick(coll: Collection<T>, compareAgainst: Collection<T>): T {
        return pick(coll, compareAgainst, 1).first()
    }

    fun <T> pick(coll: Collection<T>, compareAgainst: Collection<T> = listOf(), count: Int): MutableList<T> {
        var list = coll.toMutableList()
        val picked = mutableListOf<T>()

        if (coll.isEmpty()) return list

        while (picked.size < count) {
            if (list.isEmpty()) {
                list = coll.toMutableList()
            }

            var index = int0(list.size)
            var newElem = list.elementAt(index)

            while (compareAgainst.contains(newElem)) {
                index = int0(list.size)
                newElem = list.elementAt(index)
            }

            picked.add(list[index])
            list.removeAt(index)
        }

        return picked
    }

    fun gaussian(mean: Double = 0.0, standardDeviation: Double = 1.0): Double {
        return gaussian(mean, standardDeviation, rnd)
    }

    /**
     * https://en.wikipedia.org/wiki/Pareto_distribution
     */
    fun pareto(alpha: Double = 1.0): () -> Double {
        val invAlpha = 1.0 / max(alpha, 0.0)

        return {
            1.0 / (1.0 - double0()).pow(invAlpha)
        }
    }

    fun vector2(min: Double = -1.0, max: Double = 1.0): Vector2 {
        return Vector2.uniform(min, max, rnd)
    }

    fun vector3(min: Double = -1.0, max: Double = 1.0): Vector3 {
        return Vector3.uniform(min, max, rnd)
    }

    fun vector4(min: Double = -1.0, max: Double = 1.0): Vector4 {
        return Vector4.uniform(min, max, rnd)
    }

    fun perlin(x: Double, y: Double, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> perlinLinear(sd, x, y)
            Noise.QUINTIC -> perlinQuintic(sd, x, y)
            Noise.HERMIT -> perlinHermite(sd, x, y)
        }
    }

    fun perlin(position: Vector2, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> perlinLinear(sd, position)
            Noise.QUINTIC -> perlinQuintic(sd, position)
            Noise.HERMIT -> perlinHermite(sd, position)
        }
    }

    fun perlin(x: Double, y: Double, z: Double, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> perlinLinear(sd, x, y, z)
            Noise.QUINTIC -> perlinQuintic(sd, x, y, z)
            Noise.HERMIT -> perlinHermite(sd, x, y, z)
        }
    }

    fun perlin(position: Vector3, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> perlinLinear(sd, position)
            Noise.QUINTIC -> perlinQuintic(sd, position)
            Noise.HERMIT -> perlinHermite(sd, position)
        }
    }

    fun value(x: Double, y: Double, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> valueLinear(sd, x, y)
            Noise.QUINTIC -> valueQuintic(sd, x, y)
            Noise.HERMIT -> valueHermite(sd, x, y)
        }
    }

    fun value(position: Vector2, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> valueLinear(sd, position)
            Noise.QUINTIC -> valueQuintic(sd, position)
            Noise.HERMIT -> valueHermite(sd, position)
        }
    }

    fun value(x: Double, y: Double, z: Double, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> valueLinear(sd, x, y, z)
            Noise.QUINTIC -> valueQuintic(sd, x, y, z)
            Noise.HERMIT -> valueHermite(sd, x, y, z)
        }
    }

    fun value(position: Vector3, type: Noise = Noise.LINEAR): Double {
        val sd = stringToInt(seed)

        return when (type) {
            Noise.LINEAR -> valueLinear(sd, position)
            Noise.QUINTIC -> valueQuintic(sd, position)
            Noise.HERMIT -> valueHermite(sd, position)
        }
    }

    fun simplex(x: Double, y: Double): Double {
        return simplex(stringToInt(seed), x, y)
    }

    fun simplex(position: Vector2): Double {
        return simplex(stringToInt(seed), position)
    }

    fun simplex(x: Double, y: Double, z: Double): Double {
        return simplex(stringToInt(seed), x, y, z)
    }

    fun simplex(position: Vector3): Double {
        return simplex(stringToInt(seed), position)
    }

    fun simplex(x: Double, y: Double, z: Double, w: Double): Double {
        return simplex(stringToInt(seed), x, y, z, w)
    }

    fun simplex(position: Vector4): Double {
        return simplex(stringToInt(seed), position)
    }

    fun fbm(x: Double, y: Double, noiseFun: (Int, Double, Double) -> Double, type: Fractal = Fractal.FBM,
            octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
        val s = stringToInt(seed)

        return when (type) {
            Fractal.FBM -> orxFbm(s, x, y, noiseFun, octaves, lacunarity, gain)
            Fractal.RIGID -> rigid(s, x, y, noiseFun, octaves, lacunarity, gain)
            Fractal.BILLOW -> billow(s, x, y, noiseFun, octaves, lacunarity, gain)
        }
    }

    fun fbm(position: Vector2, noiseFun: (Int, Double, Double) -> Double,
            type: Fractal = Fractal.FBM,
            octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
        val s = stringToInt(seed)

        return when (type) {
            Fractal.FBM -> orxFbm(s, position, noiseFun, octaves, lacunarity, gain)
            Fractal.RIGID -> rigid(s, position, noiseFun, octaves, lacunarity, gain)
            Fractal.BILLOW -> billow(s, position, noiseFun, octaves, lacunarity, gain)
        }
    }

    fun fbm(x: Double, y: Double, z: Double, noiseFun: (Int, Double, Double, Double) -> Double, type: Fractal = Fractal.FBM,
            octaves: Int = 8, lacunarity: Double = 0.5, gain: Double = 0.5): Double {
        val s = stringToInt(seed)

        return when (type) {
            Fractal.FBM -> orxFbm(s, x, y, z, noiseFun, octaves, lacunarity, gain)
            Fractal.RIGID -> rigid(s, x, y, z, noiseFun, octaves, lacunarity, gain)
            Fractal.BILLOW -> billow(s, x, y, z, noiseFun, octaves, lacunarity, gain)
        }
    }

    fun fbm(position: Vector3, noiseFun: (Int, Double, Double, Double) -> Double,
            type: Fractal = Fractal.FBM, octaves: Int = 8,
            lacunarity: Double = 0.5, gain: Double = 0.5): Double {
        val s = stringToInt(seed)

        return when (type) {
            Fractal.FBM -> orxFbm(s, position, noiseFun, octaves, lacunarity, gain)
            Fractal.RIGID -> rigid(s, position, noiseFun, octaves, lacunarity, gain)
            Fractal.BILLOW -> billow(s, position, noiseFun, octaves, lacunarity, gain)
        }
    }

    fun cubic(x: Double, y: Double): Double {
        return cubic(stringToInt(seed), x, y)
    }

    fun cubic(position: Vector2): Double {
        return cubic(stringToInt(seed), position)
    }

    fun cubic(x: Double, y: Double, z: Double): Double {
        return cubic(stringToInt(seed), x, y, z)
    }

    fun cubic(position: Vector3):Double {
        return cubic(stringToInt(seed), position)
    }

    fun ring2d(innerRadius: Double = 0.0, outerRadius: Double = 1.0, count: Int = 1): Any {
        return when (count) {
            1 -> Vector2.uniformRing(innerRadius, outerRadius, rnd)
            else -> Vector2.uniformsRing(count, innerRadius, outerRadius, rnd)
        }
    }

    fun ring3d(innerRadius: Double = 0.0, outerRadius: Double = 1.0, count: Int = 1): Any {
        return when (count) {
            1 -> Vector3.uniformRing(innerRadius, outerRadius, rnd)
            else -> Vector3.uniformsRing(count, innerRadius, outerRadius, rnd)
        }
    }

    fun <T> roll(elements: Collection<T>, distribution: (Int) -> List<Double>): T {
        val result = double0()
        val probabilities = distribution(elements.size)
        val index = probabilities.indexOfFirst { result <= it }

        return elements.elementAtOrNull(index) ?: elements.last()
    }

    fun point(rect: Rectangle): Vector2 {
        return rect.position(vector2(0.0, 1.0))
    }
}

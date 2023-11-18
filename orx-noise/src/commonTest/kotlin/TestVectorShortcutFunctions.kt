import io.kotest.core.spec.style.DescribeSpec
import org.openrndr.extra.noise.Random
import org.openrndr.extra.noise.perlinQuintic
import org.openrndr.math.Vector4

import kotlin.test.assertEquals

class TestVectorShortcutFunctions : DescribeSpec({

    val v = Vector4(1.13, 2.74, 3.59, 4.83)

    describe("perlin with Vector2") {
        it("produces expected result") {
            assertEquals(Random.perlin(v.x, v.y, Random.Noise.QUINTIC),
                    Random.perlin(v.xy, Random.Noise.QUINTIC))
        }
    }

    describe("perlin with Vector3") {
        it("produces expected result") {
            assertEquals(Random.perlin(v.x, v.y, v.z, Random.Noise.QUINTIC),
                    Random.perlin(v.xyz, Random.Noise.QUINTIC))
        }
    }

    // ---

    describe("value with Vector2") {
        it("produces expected result") {
            assertEquals(Random.value(v.x, v.y, Random.Noise.QUINTIC),
                    Random.value(v.xy, Random.Noise.QUINTIC))
        }
    }

    describe("value with Vector3") {
        it("produces expected result") {
            assertEquals(Random.value(v.x, v.y, v.z, Random.Noise.QUINTIC),
                    Random.value(v.xyz, Random.Noise.QUINTIC))
        }
    }

    // ---

    describe("simplex with Vector2") {
        it("produces expected result") {
            assertEquals(Random.simplex(v.x, v.y), Random.simplex(v.xy))
        }
    }

    describe("simplex with Vector3") {
        it("produces expected result") {
            assertEquals(Random.simplex(v.x, v.y, v.z), Random.simplex(v.xyz))
        }
    }

    describe("simplex with Vector4") {
        it("produces expected result") {
            assertEquals(Random.simplex(v.x, v.y, v.z, v.w),
                    Random.simplex(v))
        }
    }

    // ---

    describe("fbm with Vector2") {
        it("produces expected result") {
            assertEquals(Random.fbm(v.x, v.y, ::perlinQuintic),
                    Random.fbm(v.xy, ::perlinQuintic))
        }
    }

    describe("fbm with Vector3") {
        it("produces expected result") {
            assertEquals(Random.fbm(v.x, v.y, v.z, ::perlinQuintic),
                    Random.fbm(v.xyz, ::perlinQuintic))
        }
    }

    // ---

    describe("cubic with Vector2") {
        it("produces expected result") {
            assertEquals(Random.cubic(v.x, v.y), Random.cubic(v.xy))
        }
    }

    describe("cubic with Vector3") {
        it("produces expected result") {
            assertEquals(Random.cubic(v.x, v.y, v.z), Random.cubic(v.xyz))
        }
    }

})
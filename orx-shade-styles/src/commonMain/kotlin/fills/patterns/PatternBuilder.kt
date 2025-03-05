package org.openrndr.extra.shadestyles.fills.patterns

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ObservableHashmap
import org.openrndr.draw.StyleParameters
import org.openrndr.extra.shadestyles.fills.FillFit
import org.openrndr.extra.shadestyles.fills.FillUnits
import org.openrndr.math.Matrix44

class PatternBuilder : StyleParameters {
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) {}
    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var textureBaseIndex: Int = 2

    var filterWindow: Int by Parameter("patternFilterWindow", 5)
    var filterSpread: Double by Parameter("patternFilterSpread", 1.0)

    var foregroundColor: ColorRGBa by Parameter("patternForegroundColor", ColorRGBa.BLACK)
    var backgroundColor: ColorRGBa by Parameter("patternBackgroundColor", ColorRGBa.WHITE)

    var patternUnits: FillUnits = FillUnits.BOUNDS
    var patternFit: FillFit = FillFit.STRETCH
    var patternFunction = """float pattern(vec2 coord) { return 1.0; }"""
    var domainWarpFunction = """vec2 patternDomainWarp(vec2 coord) { return coord; }"""
    var patternTransform: Matrix44 by Parameter("patternTransform", Matrix44.IDENTITY)
    var invert: Boolean by Parameter("patternInvert", false)
    var scale: Double by Parameter("patternScale", 1.0)

    fun build(): PatternBase {
        val structure = PatternBaseStructure(patternFunction, domainWarpFunction)
        val patternBase = PatternBase(structure)
        patternBase.parameterTypes.putAll(parameterTypes)
        patternBase.parameterValues.putAll(parameterValues)
        patternBase.patternUnits = patternUnits.ordinal
        patternBase.patternFit = patternFit.ordinal
        return patternBase
    }

    /**
     * Configures and applies the checkers pattern to the current pattern builder.
     *
     * @param builder The lambda that defines customization for the CheckerPatternBuilder.
     */
    fun checkers(builder: CheckerPatternBuilder.() -> Unit) {
        val checkerBuilder = CheckerPatternBuilder(this)
        checkerBuilder.builder()
    }

    /**
     * Configures and applies the XOR Modulation pattern to the current pattern builder.
     *
     * @param builder A lambda scope that defines customization for the XorModPatternBuilder.
     */
    fun xorMod(builder: XorModPatternBuilder.() -> Unit) {
        val xorModBuilder = XorModPatternBuilder(this)
        xorModBuilder.builder()
    }

    /**
     * Configures and applies the XOR Modulation 2 pattern to the current pattern builder.
     *
     * @param builder A lambda scope that defines customization for the XorMod2PatternBuilder.
     */
    fun xorMod2(builder: XorMod2PatternBuilder.() -> Unit) {
        val xorModBuilder = XorMod2PatternBuilder(this)
        xorModBuilder.builder()
    }

    /**
     * Configures and applies the dots pattern to the current pattern builder.
     *
     * @param builder A lambda scope that defines customization for the DotsPatternBuilder.
     */
    fun dots(builder: DotsPatternBuilder.() -> Unit) {
        val dotsPatternBuilder = DotsPatternBuilder(this)
        dotsPatternBuilder.builder()
    }

    /**
     * Configures and applies the boxes pattern to the current pattern builder.
     *
     * @param builder A lambda scope that defines customization for the BoxPatternBuilder.
     */
    fun boxes(builder: BoxPatternBuilder.() -> Unit) {
        val boxPatternBuilder = BoxPatternBuilder(this)
        boxPatternBuilder.builder()
    }

    /**
     * Configures and applies the crosses pattern to the current pattern builder.
     *
     * @param builder A lambda scope that defines customization for the CrossPatternBuilder.
     */
    fun crosses(builder: CrossPatternBuilder.() -> Unit) {
        val crossPatternBuilder = CrossPatternBuilder(this)
        crossPatternBuilder.builder()
    }
}


class CheckerPatternBuilder(builder: PatternBuilder) {
    init {
        builder.patternFunction = """float pattern(vec2 coord) { return mod(floor(coord.x)+floor(coord.y), 2.0);}"""
    }
}

class XorModPatternBuilder(builder: PatternBuilder) {
    var patternMod: Int by builder.Parameter("patternMod", 9)
    var patternMask: Int by builder.Parameter("patternMask", 3)
    init {
        builder.patternFunction = """float pattern(vec2 coord) {
            ivec2 icoord = ivec2(floor(coord * p_patternScale));
            int i = ((icoord.x ^ icoord.y) % p_patternMod) & p_patternMask;
            return i == 0 ? 0.0 : 1.0;                                                 
        }""".trimIndent()
    }
}

class XorMod2PatternBuilder(builder: PatternBuilder) {
    var patternMod: Int by builder.Parameter("patternMod", 9)
    var patternMask: Int by builder.Parameter("patternMask", 3)
    var patternOffset: Int by builder.Parameter("patternOffset", 0)
    init {
        builder.patternFunction = """float pattern(vec2 coord) {
            ivec2 icoord = ivec2(floor(coord * p_patternScale));
            int i = (icoord.x + icoord.y) ^ (icoord.x - icoord.y);
            int i3 = i * i * i;
            int i6 = i3 * i3;
            int i7 = i * i6;
            return (( (i7 + p_patternOffset) % int(p_patternMod)) & int(p_patternMask)) == 0 ? 0.0 : 1.0;                                                 
        }""".trimIndent()
    }
}

class DotsPatternBuilder(builder: PatternBuilder) {
    var dotSize: Double by builder.Parameter("patternDotSize", 0.25)
    var strokeWeight: Double by builder.Parameter("patternStrokeWeight", 1E10)
    init {
        builder.patternFunction = """float pattern(vec2 coord) {
            vec2 scoord = coord * p_patternScale;
            vec2 mcoord = mod(scoord  + vec2(0.5), vec2(1.0)) - vec2(0.5);
            float d = length(mcoord) - p_patternDotSize;
            float dw = fwidth(d);
            return smoothstep(dw/2.0, -dw/2.0, d) * smoothstep(-dw/2.0, dw/2.0, d+p_patternStrokeWeight);;
        }""".trimIndent()
    }
}

class BoxPatternBuilder(builder: PatternBuilder) {
    var width: Double by builder.Parameter("patternBoxWidth", 0.5)
    var height: Double by builder.Parameter("patternBoxHeight", 0.5)
    var rounding: Double by builder.Parameter("patternBoxRounding", 0.0)
    var rotation: Double by builder.Parameter("patternBoxRotation", 0.0)
    var strokeWeight: Double by builder.Parameter("patternStrokeWeight", 1E10)
    init {
        builder.patternFunction = """float pattern(vec2 coord) {
            float phi = p_patternBoxRotation / 180.0 * 3.141592654;
            mat2 rm = mat2(cos(phi), sin(phi), -sin(phi), cos(phi));
            vec2 mcoord = mod(coord * p_patternScale + vec2(0.5), vec2(1.0)) - vec2(0.5);
            mcoord = rm * mcoord;
            vec2 d2 = abs(mcoord) - vec2(p_patternBoxWidth - p_patternBoxRounding, p_patternBoxHeight - p_patternBoxRounding)*0.5;
            float d = length(max(d2,0.0)) + min(max(d2.x,d2.y),0.0) - p_patternBoxRounding;
            float dw = fwidth(d);
            return smoothstep(dw/2.0, -dw/2.0, d) * smoothstep(-dw/2.0, dw/2.0, d+p_patternStrokeWeight);;
        }""".trimIndent()
    }
}

class CrossPatternBuilder(builder: PatternBuilder) {
    var width: Double by builder.Parameter("patternCrossWidth", 0.5)
    var weight: Double by builder.Parameter("patternCrossRounding", 0.1)
    var rotation: Double by builder.Parameter("patternCrossRotation", 0.0)
    var strokeWeight: Double by builder.Parameter("patternStrokeWeight", 1E10)
    init {
        builder.patternFunction = """float pattern(vec2 coord) {
            float phi = p_patternCrossRotation / 180.0 * 3.141592654;
            mat2 rm = mat2(cos(phi), sin(phi), -sin(phi), cos(phi));
            vec2 mcoord = mod(coord * p_patternScale + vec2(0.5), vec2(1.0)) - vec2(0.5);
            mcoord = rm * mcoord;
            vec2 p = abs(mcoord);
            float d = length(p-min(p.x+p.y, p_patternCrossWidth)*0.5) - p_patternCrossRounding;
            
            float dw = fwidth(d);
            return smoothstep(dw/2.0, -dw/2.0, d) * smoothstep(-dw/2.0, dw/2.0, d+p_patternStrokeWeight);;
        }""".trimIndent()
    }
}

/**
 * Creates and returns a new `PatternBase` instance configured using the provided builder function.
 *
 * @param builder A lambda that operates on a `PatternBuilder` instance to configure the pattern's properties.
 * @return A `PatternBase` instance, representing the configured pattern with the applied settings.
 */
fun pattern(builder: PatternBuilder.() -> Unit): PatternBase {
    val patternBuilder = PatternBuilder()
    patternBuilder.builder()
    return patternBuilder.build()
}
package org.openrndr.extra.fcurve

/**
 * Represents a collection of named `FCurve` objects, enabling the manipulation and
 * querying of multiple functional curves as a unified entity. Each `FCurve` in the
 * collection is identified by a unique string key, allowing structured access and control.
 *
 * @param compounds A map containing string keys associated with `FCurve` instances.
 */
open class MultiFCurve(val compounds: Map<String, FCurve?>) {
    fun changeSpeed(speed: Double): MultiFCurve {
        return if (speed == 1.0) {
            this
        } else {
            MultiFCurve(compounds.mapValues { it.value?.changeSpeed(speed) })
        }
    }

    /**
     * Duration of the [MultiFCurve]
     */
    val duration by lazy { compounds.values.maxOfOrNull { it?.duration ?: 0.0 } ?: 0.0 }


    /**
     * Start position of the [MultiFCurve]
     */
    val start by lazy { compounds.values.minOfOrNull { it?.start ?: 0.0 } ?: 0.0 }

    /**
     * End position of the [MultiFCurve]
     */
    val end by lazy { compounds.values.maxOfOrNull { it?.end ?: 0.0 } ?: 0.0 }

    operator fun get(name: String): FCurve? {
        return compounds[name]
    }
}


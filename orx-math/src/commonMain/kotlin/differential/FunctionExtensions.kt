package org.openrndr.extra.math.differential

import org.openrndr.math.LinearType
import kotlin.jvm.JvmName

@JvmName("diffDoubleLinearType")
fun <T : LinearType<T>> ((Double) -> T).diff(eps: Double = 1e-4): (Double) -> T = {
    (this(it + eps) - this(it - eps)) / (2.0 * eps)
}

@JvmName("diffDoubleDouble")
fun ((Double) -> Double).diff(eps: Double = 1e-4): (Double) -> Double = {
    (this(it + eps) - this(it - eps)) / (2.0 * eps)
}

@JvmName("diffdxDoubleDoubleLinearType")
fun <T : LinearType<T>> ((Double, Double) -> T).diffdx(eps: Double = 1e-4): (Double, Double) -> T =
    { x: Double, y: Double ->
        (this(x + eps, y) - this(x - eps, y)) / (2.0 * eps)
    }

@JvmName("diffdyDoubleDoubleLinearType")
fun <T : LinearType<T>> ((Double, Double) -> T).diffdy(eps: Double = 1e-4): (Double, Double) -> T =
    { x: Double, y: Double ->
        (this(x, y + eps) - this(x, y - eps)) / (2.0 * eps)
    }

@JvmName("diffdxDoubleDoubleDouble")
fun ((Double, Double) -> Double).diffdx(eps: Double = 1e-4): (Double, Double) -> Double =
    { x: Double, y: Double ->
        (this(x + eps, y) - this(x - eps, y)) / (2.0 * eps)
    }

@JvmName("diffdyDoubleDoubleDouble")
fun ((Double, Double) -> Double).diffdy(eps: Double = 1e-4): (Double, Double) -> Double =
    { x: Double, y: Double ->
        (this(x, y + eps) - this(x, y - eps)) / (2.0 * eps)
    }

//

@JvmName("diffdxDoubleDoubleDoubleLinearType")
fun <T : LinearType<T>> ((Double, Double, Double) -> T).diffdx(eps: Double = 1e-4): (Double, Double, Double) -> T =
    { x: Double, y: Double, z: Double ->
        (this(x + eps, y, z) - this(x - eps, y, z)) / (2.0 * eps)
    }

@JvmName("diffdyDoubleDoubleDoubleLinearType")
fun <T : LinearType<T>> ((Double, Double, Double) -> T).diffdy(eps: Double = 1e-4): (Double, Double, Double) -> T =
    { x, y, z ->
        (this(x, y + eps, z) - this(x, y - eps, z)) / (2.0 * eps)
    }

@JvmName("diffdzDoubleDoubleDoubleLinearType")
fun <T : LinearType<T>> ((Double, Double, Double) -> T).diffdz(eps: Double = 1e-4): (Double, Double, Double) -> T =
    { x, y, z ->
        (this(x, y, z + eps) - this(x, y, z - eps)) / (2.0 * eps)
    }

//

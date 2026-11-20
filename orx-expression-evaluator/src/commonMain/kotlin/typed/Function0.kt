package org.openrndr.extra.expressions.typed

import org.openrndr.extra.noise.uniform

/**
 * Dispatches a function without arguments based on its name.
 *
 * @param name The name of the function to dispatch.
 * @param functions A map containing functions of type `TypedFunction0` associated with their names.
 * @return A callable lambda that takes an array of `Any` as input and returns a result if the function is found,
 *         or null if there is no match.
 */
internal fun dispatchFunction0(name: String, functions: Map<String, TypedFunction0>): ((Array<Any>) -> Any)? {
    return when (name) {
        "random" -> { x -> Double.uniform(0.0, 1.0) }
        else -> functions[name]?.let { { x: Array<Any> -> it.invoke() } }
    }
}
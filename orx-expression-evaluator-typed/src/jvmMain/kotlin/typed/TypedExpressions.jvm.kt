package org.openrndr.extra.expressions.typed

import kotlin.concurrent.getOrSet

/*
Thread safe TypeExpressionListener
 */
actual class TypedExpressionListener actual constructor(
    functions: TypedFunctionExtensions,
    constants: (String) -> Any?
) : TypedExpressionListenerBase(functions, constants) {
    private val threadLocalState = ThreadLocal<State>()
    actual override val state: State
        get() = threadLocalState.getOrSet { State() }
}
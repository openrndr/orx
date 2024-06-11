package org.openrndr.extra.expressions.typed

actual class TypedExpressionListener actual constructor(
    functions: TypedFunctionExtensions,
    constants: (String) -> Any?
) :  TypedExpressionListenerBase(functions, constants) {
    actual override val state: State = State()
}
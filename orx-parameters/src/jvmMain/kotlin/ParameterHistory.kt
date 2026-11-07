package org.openrndr.extra.parameters

/**
 * The history of the changes to the values of [bindings], for undo and redo. Changes that come in quick succession are
 * one step: the changes of a drag of a control to the same parameter, and the changes that are made at once, like by
 * [ParameterBindings.randomize] or [ParameterBindings.load].
 *
 * @param maxSteps the most steps that are kept
 * @param clock the time in seconds, real time by default
 */
class ParameterHistory(
    val bindings: ParameterBindings,
    val maxSteps: Int = 100,
    private val clock: () -> Double = { System.nanoTime() / 1E9 }
) {
    private class Change(val binding: ParameterBindings.Binding, val parameter: Parameter, val before: ParameterValue, var after: ParameterValue)

    private class Step(val changes: MutableList<Change>, val start: Double, var last: Double)

    private val undoSteps = ArrayDeque<Step>()
    private val redoSteps = ArrayDeque<Step>()

    /** the values the parameters had after the last change, the values they change from */
    private val known = java.util.IdentityHashMap<ParameterBindings.Binding, MutableMap<Parameter, ParameterValue>>()

    /** true while the history sets values, those changes are not recorded */
    private var restoring = false

    /** changes within this time from the start of a step are part of it, in seconds */
    var batchTime = 0.05

    /** changes to the same parameter within this time from the last change are part of its step, in seconds */
    var dragTime = 1.0

    val canUndo: Boolean get() = undoSteps.isNotEmpty()
    val canRedo: Boolean get() = redoSteps.isNotEmpty()

    private fun knownValue(binding: ParameterBindings.Binding, parameter: Parameter): ParameterValue? =
        known[binding]?.get(parameter)

    private fun remember(binding: ParameterBindings.Binding) {
        known[binding] = binding.valueParameters.filter { it.property != null }
            .associateWith { bindings.parameterValue(binding, it) }.toMutableMap()
    }

    init {
        bindings.all().forEach(::remember)
        bindings.added.listen { added -> bindings.all().filter { known[it] == null }.forEach(::remember) }
        bindings.rebound.listen { bindings.all().filter { known[it] == null }.forEach(::remember) }
        bindings.changed.listen { change -> record(change.binding, change.parameter) }
    }

    private fun record(binding: ParameterBindings.Binding, parameter: Parameter) {
        if (restoring || parameter.property == null || parameter.parameterType == ParameterType.Nested) return
        val before = knownValue(binding, parameter) ?: return
        val after = bindings.parameterValue(binding, parameter)
        known.getOrPut(binding) { mutableMapOf() }[parameter] = after
        if (sameValue(before, after)) return
        val now = clock()
        val last = undoSteps.lastOrNull()
        val change = last?.changes?.find { it.binding === binding && it.parameter === parameter }
        val joins = last != null && (now - last.start < batchTime ||
                (last.changes.size == 1 && change != null && now - last.last < dragTime))
        if (joins) {
            if (change != null) change.after = after else last!!.changes.add(Change(binding, parameter, before, after))
            last!!.last = now
        } else {
            undoSteps.addLast(Step(mutableListOf(Change(binding, parameter, before, after)), now, now))
            while (undoSteps.size > maxSteps) undoSteps.removeFirst()
        }
        redoSteps.clear()
    }

    private fun sameValue(a: ParameterValue, b: ParameterValue) = a.sameAs(b)

    /** sets the values of the changes of [step], before or after them */
    private fun restore(step: Step, before: Boolean) {
        restoring = true
        try {
            val bound = bindings.all().toSet()
            for (change in if (before) step.changes.asReversed() else step.changes) {
                if (change.binding !in bound) continue
                val value = if (before) change.before else change.after
                bindings.setParameterValue(change.binding, change.parameter, value)
                known.getOrPut(change.binding) { mutableMapOf() }[change.parameter] = value
            }
        } finally {
            restoring = false
        }
    }

    /** undoes the last step, true when there was one */
    fun undo(): Boolean {
        val step = undoSteps.removeLastOrNull() ?: return false
        restore(step, before = true)
        redoSteps.addLast(step)
        return true
    }

    /** redoes the last step that was undone, true when there was one */
    fun redo(): Boolean {
        val step = redoSteps.removeLastOrNull() ?: return false
        restore(step, before = false)
        undoSteps.addLast(step)
        return true
    }

    /** forgets all steps */
    fun clear() {
        undoSteps.clear()
        redoSteps.clear()
    }
}

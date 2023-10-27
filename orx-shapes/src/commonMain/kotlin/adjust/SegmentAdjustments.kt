package org.openrndr.extra.shapes.adjust

import org.openrndr.shape.Segment

sealed interface SegmentOperation {
    data class Remove(val index: Int, val amount: Int) : SegmentOperation
    data class Insert(val index: Int, val amount: Int) : SegmentOperation
}


class SegmentAdjustments(
    val replacements: List<Triple<Int, Segment, Segment>>,
    val operations: List<SegmentOperation>
) {

    companion object {
        val EMPTY = SegmentAdjustments(emptyList(), emptyList())
    }
}

class SegmentAdjuster(val list: MutableList<Segment>) {
    val adjustments = mutableListOf<SegmentOperation>()

    fun removeAt(index: Int) {
        list.removeAt(index)
        adjustments.add(SegmentOperation.Remove(index, 1))
    }
    fun add(segment: Segment) {
        list.add(segment)
        adjustments.add(SegmentOperation.Insert(list.lastIndex, 1))
    }
    fun add(index: Int, segment: Segment) {
        list.add(index, segment)
        adjustments.add(SegmentOperation.Insert(index, 1))
    }
}

fun MutableList<Segment>.adjust(block: SegmentAdjuster.() -> Unit) : List<SegmentOperation> {
    val adjuster = SegmentAdjuster(this)
    adjuster.block()
    return adjuster.adjustments
}
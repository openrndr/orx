package org.openrndr.extra.mediapipe

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/** a detection of an SSD model, before it is placed in the image: in the coordinates of the input, from 0 to 1 */
internal class RawDetection(val box: Rectangle, val keypoints: List<Vector2>, val score: Double)

/**
 * The centers of the anchors of an SSD model with fixed anchor sizes, like MediaPipe's SsdAnchorsCalculator: the layers
 * of the same stride share their cells, every layer has [anchorsPerLayer] anchors in each cell.
 */
internal fun ssdAnchors(inputSize: Int, strides: List<Int>, anchorsPerLayer: Int = 2): List<Vector2> {
    val anchors = mutableListOf<Vector2>()
    var layer = 0
    while (layer < strides.size) {
        val stride = strides[layer]
        var layers = 0
        while (layer < strides.size && strides[layer] == stride) {
            layers++
            layer++
        }
        val cells = ceil(inputSize.toDouble() / stride).toInt()
        for (y in 0 until cells) for (x in 0 until cells) {
            val center = Vector2((x + 0.5) / cells, (y + 0.5) / cells)
            repeat(layers * anchorsPerLayer) { anchors.add(center) }
        }
    }
    return anchors
}

/**
 * The detections in [boxes], for each anchor its center, size and keypoints in pixels of the input relative to the
 * anchor, with the scores in [scores] before the sigmoid, of [minScore] and more
 */
internal fun decodeDetections(
    boxes: FloatArray,
    scores: FloatArray,
    anchors: List<Vector2>,
    inputSize: Int,
    keypoints: Int,
    minScore: Double
): List<RawDetection> {
    val stride = boxes.size / anchors.size
    val s = inputSize.toDouble()
    return anchors.indices.mapNotNull { i ->
        val score = 1.0 / (1.0 + exp(-scores[i].toDouble().coerceIn(-100.0, 100.0)))
        if (score < minScore) return@mapNotNull null
        val o = i * stride
        val anchor = anchors[i]
        val cx = boxes[o] / s + anchor.x
        val cy = boxes[o + 1] / s + anchor.y
        val w = boxes[o + 2] / s
        val h = boxes[o + 3] / s
        RawDetection(
            Rectangle(cx - w / 2.0, cy - h / 2.0, w, h),
            (0 until keypoints).map { k -> Vector2(boxes[o + 4 + 2 * k] / s + anchor.x, boxes[o + 5 + 2 * k] / s + anchor.y) },
            score
        )
    }
}

/** the intersection over the union of [a] and [b] */
internal fun iou(a: Rectangle, b: Rectangle): Double {
    val w = min(a.x + a.width, b.x + b.width) - max(a.x, b.x)
    val h = min(a.y + a.height, b.y + b.height) - max(a.y, b.y)
    if (w <= 0.0 || h <= 0.0) return 0.0
    val intersection = w * h
    return intersection / (a.width * a.height + b.width * b.height - intersection)
}

/**
 * Weighted non-maximum suppression, like MediaPipe's: the detections that overlap the best remaining detection by more
 * than [threshold] are merged into it, their boxes and keypoints weighted by their scores, with its score
 */
internal fun weightedNonMaxSuppression(detections: List<RawDetection>, threshold: Double): List<RawDetection> {
    var remaining = detections.sortedByDescending { it.score }
    val result = mutableListOf<RawDetection>()
    while (remaining.isNotEmpty()) {
        val top = remaining.first()
        val (candidates, rest) = remaining.partition { it === top || iou(top.box, it.box) > threshold }
        remaining = rest
        val total = candidates.sumOf { it.score }
        fun weighted(f: (RawDetection) -> Double) = candidates.sumOf { f(it) * it.score } / total
        val x0 = weighted { it.box.x }
        val y0 = weighted { it.box.y }
        val x1 = weighted { it.box.x + it.box.width }
        val y1 = weighted { it.box.y + it.box.height }
        result.add(
            RawDetection(
                Rectangle(x0, y0, x1 - x0, y1 - y0),
                top.keypoints.indices.map { k -> Vector2(weighted { it.keypoints[k].x }, weighted { it.keypoints[k].y }) },
                top.score
            )
        )
    }
    return result
}

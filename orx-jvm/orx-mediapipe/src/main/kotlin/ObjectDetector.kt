package org.openrndr.extra.mediapipe

import org.openrndr.draw.ColorBuffer
import org.openrndr.shape.Rectangle
import java.io.File
import kotlin.math.exp

/** an object found by [ObjectDetector], its bounds in the pixels of the image and its category */
class ObjectDetection(val bounds: Rectangle, val category: Category)

/**
 * Finds objects of the 80 categories of COCO in images like MediaPipe's object detector, with `efficientdet_lite0.onnx`,
 * the slower and more precise `efficientdet_lite2.onnx` or `ssd_mobilenet_v2.onnx`: the image is stretched over the
 * input of the model, the boxes of its anchors are decoded and those that overlap better ones are left out.
 * @param scoreThreshold the least score of an object; MediaPipe finds objects of all scores by default
 * @param maxResults the most objects that are found, all when negative
 * @param suppressionThreshold the overlap from which objects are taken to be the same object
 */
class ObjectDetector(
    modelPath: String = MediaPipeModels.model("efficientdet_lite0.onnx"),
    var scoreThreshold: Double = 0.3,
    var maxResults: Int = -1,
    var suppressionThreshold: Double = 0.3
) : AutoCloseable {
    private val model = OnnxModel(modelPath)

    /** the labels of the categories, `???` for those that are not used */
    val labels = labels(modelPath)

    // the anchors and the options of the decoding of the boxes, from the metadata of the model
    private val options: Map<String, String>
    private val anchors: FloatArray

    init {
        val file = File(modelPath.removeSuffix(".onnx") + ".detector.txt")
        require(file.exists()) { "no anchors at '$file'; MediaPipeModels.model() downloads the models of orx-models, tools/convert_models.py converts them" }
        val (head, tail) = file.readText().split("anchors\n")
        options = head.lines().filter { it.isNotBlank() }.associate { it.substringBefore(' ') to it.substringAfter(' ') }
        anchors = tail.lines().filter { it.isNotBlank() }.flatMap { line -> line.split(' ').map { it.toFloat() } }.toFloatArray()
    }

    private val xScale = options.getValue("x_scale").toFloat()
    private val yScale = options.getValue("y_scale").toFloat()
    private val wScale = options.getValue("w_scale").toFloat()
    private val hScale = options.getValue("h_scale").toFloat()
    private val exponential = options.getValue("apply_exponential_on_box_size") == "1"
    private val sigmoid = options.getValue("sigmoid_score") == "1"

    /** the categories that are found, not the unused ones nor the background */
    private val found = labels.indices.filter { labels[it] != "???" && labels[it] != "background" }

    /** the objects in [image], the most likely first */
    fun detect(image: RgbaImage): List<ObjectDetection> {
        val toImage = Affine.stretch(image.width, image.height, model.width, model.height)
        val outputs = model.run(image.tensor(model.width, model.height, toImage, -1f, 1f)).values
        val count = anchors.size / 4
        val boxes = outputs.single { it.size == count * 4 }
        val scores = outputs.single { it !== boxes }
        val classes = scores.size / count

        class Candidate(val bounds: Rectangle, val index: Int, val score: Double)
        val candidates = ArrayList<Candidate>()
        for (i in 0 until count) {
            var best = -1
            var bestScore = Double.NEGATIVE_INFINITY
            for (c in found) {
                val s = scores[i * classes + c].toDouble()
                if (s > bestScore) {
                    bestScore = s
                    best = c
                }
            }
            if (sigmoid) bestScore = 1.0 / (1.0 + exp(-bestScore))
            if (bestScore < scoreThreshold) continue
            // the box relative to its anchor, y, x, height and width, clipped to the image
            val ax = anchors[4 * i]
            val ay = anchors[4 * i + 1]
            val aw = anchors[4 * i + 2]
            val ah = anchors[4 * i + 3]
            val yc = boxes[4 * i] / yScale * ah + ay
            val xc = boxes[4 * i + 1] / xScale * aw + ax
            val h = if (exponential) exp(boxes[4 * i + 2] / hScale) * ah else boxes[4 * i + 2] / hScale * ah
            val w = if (exponential) exp(boxes[4 * i + 3] / wScale) * aw else boxes[4 * i + 3] / wScale * aw
            val x0 = (xc - w / 2).coerceIn(0f, 1f) * image.width
            val y0 = (yc - h / 2).coerceIn(0f, 1f) * image.height
            val x1 = (xc + w / 2).coerceIn(0f, 1f) * image.width
            val y1 = (yc + h / 2).coerceIn(0f, 1f) * image.height
            candidates.add(Candidate(Rectangle(x0.toDouble(), y0.toDouble(), (x1 - x0).toDouble(), (y1 - y0).toDouble()), best, bestScore))
        }
        // like MediaPipe's non-maximum suppression, over all categories
        candidates.sortByDescending { it.score }
        val kept = ArrayList<Candidate>()
        for (c in candidates) {
            if (maxResults in 0..kept.size) break
            if (kept.none { iou(it.bounds, c.bounds) > suppressionThreshold }) kept.add(c)
        }
        return kept.map { ObjectDetection(it.bounds, Category(labels[it.index], it.score)) }
    }

    /** the objects in [image], the most likely first */
    fun detect(image: ColorBuffer) = detect(image.toRgbaImage())

    override fun close() = model.close()
}

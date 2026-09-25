import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession.SessionOptions
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.Linearity
import org.openrndr.extra.shapes.primitives.grid
import java.nio.FloatBuffer

fun main() {
    application {
        program {
            val env = OrtEnvironment.getEnvironment()
            env.setTelemetry(false)
            val options = SessionOptions()
            val session = env.createSession("orx-jvm/orx-onnx-runtime/test-data/decoder.onnx", options)


            val fb = FloatBuffer.allocate(800)

            for (j in 0 until 20) {
                for (i in 0 until 20) {
                    fb.put((i / 19.0f - 0.5f) * 10.1f)
                    fb.put((j / 19.0f - 0.5f) * 10.31f)
                }
            }

            fb.flip()
            val inputTensor = OnnxTensor.createTensor(env, fb, longArrayOf(400, 2))

            for (oi in session.inputInfo) {
                println(oi.key)
                println(oi.value.info)
            }

            val result = session.run(mapOf("embedding" to inputTensor))

            val outputTensor = result["reconstruction"]!!.get() as OnnxTensor
            val ofb = outputTensor.floatBuffer

            extend {
                drawer.stroke = null
                val cells = drawer.bounds.grid(20, 20).flatten()
                for ((index, cell) in cells.withIndex()) {
                    val color = ColorRGBa(
                        ofb.get(index * 3).toDouble(),
                        ofb.get(index * 3 + 1).toDouble(),
                        ofb.get(index * 3 + 2).toDouble(),
                        1.0, Linearity.LINEAR
                    )
                    drawer.fill = color
                    drawer.rectangle(cell)
                }
            }
        }
    }
}
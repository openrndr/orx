//import org.openrndr.application
//import org.openrndr.color.ColorRGBa
//import org.openrndr.draw.Writer
//import org.openrndr.draw.loadFont
//import org.openrndr.extra.runway.Gpt2Request
//import org.openrndr.extra.runway.Gpt2Result
//import org.openrndr.extra.runway.runwayQuery
//import org.openrndr.shape.Rectangle
//
//import java.io.File
//
///**
// * This demonstrates a prompt to text model. It generates a longer text sequence from a prompt.
// * This example requires a `runway/GPT-2` model to be active in Runway.
// */
//
//fun main() = application {
//    configure {
//        width = 768
//        height = 768
//    }
//
//    program {
//
//        val runwayHost = "http://localhost:8000/query"
//
//        val nouns = File("demo-data/words/nouns.txt").readText().split("\n")
//        val prepositions = File("demo-data/words/prepositions.txt").readText().split("\n")
//        val adjectives = File("demo-data/words/adjectives.txt").readText().split("\n")
//
//        val font = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 36.0)
//
//        val promptFont = loadFont("demo-data/fonts/IBMPlexMono-Regular.ttf", 24.0)
//
//        extend {
//            val prompt = "a ${adjectives.random()} ${nouns.random()} ${prepositions.random()} a ${adjectives.random()} ${nouns.random()}"
//            drawer.fontMap = promptFont
//            val wp = Writer(drawer)
//            wp.box = Rectangle(20.0, 0.0, width - 40.0, height - 40.0)
//            wp.newLine()
//            wp.text(prompt)
//
//
//            val result: Gpt2Result = runwayQuery(runwayHost, Gpt2Request(prompt, sequenceLength = 128))
//
//            drawer.fill = ColorRGBa.PINK
//            drawer.fontMap = font
//
//            val w = Writer(drawer)
//            w.box = Rectangle(20.0, 60.0, width - 40.0, height - 80.0)
//            w.newLine()
//            w.text(result.text)
//        }
//    }
//}
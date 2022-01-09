# orx-compute-graph

A graph for computation.

## Status

In development. Things may change without prior notice.

## Usage

```
fun main() = application {
    program {
        val linkNode: ComputeNode
        val cg = computeGraph {
            val randomNode = node {
                var seed: Int by inputs
                seed = 0
                var points: List<Vector2> by outputs
                points = emptyList()
                compute {
                    val r = Random(seed)
                    points = (0 until 1000).map {
                        val x = r.nextDouble(0.0, width.toDouble())
                        val y = r.nextDouble(0.0, height.toDouble())
                        Vector2(x, y)
                    }
                }
            }
            linkNode = node {
                var seed: Int by inputs
                seed = 0
                val points: List<Vector2> by randomNode.outputs
                var links: List<LineSegment> by outputs
                compute {
                    val r = Random(seed)
                    val shuffled = points.shuffled(r)
                    links = shuffled.windowed(2, 2).map { 
                        LineSegment(it[0], it[1])
                    }
                }
            }
            randomNode.dependOn(root)
            linkNode.dependOn(randomNode)
        }
        cg.dispatch(dispatcher) {}
        extend {
            drawer.clear(ColorRGBa.WHITE)
            drawer.stroke = ColorRGBa.BLACK
            val links: List<LineSegment> by linkNode.outputs
            drawer.lineSegments(links)
        }
    }
}
```
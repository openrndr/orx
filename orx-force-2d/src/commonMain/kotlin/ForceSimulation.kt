package org.openrndr.extra.force2d
import kotlinx.coroutines.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

class Node(
    var position: Vector2,
    var prevPosition: Vector2,
    var velocity: Vector2,
    var inverseMass: Double = 1.0,
    var radius: Double = 0.0,
    var id: Int = 0
) {
    val bounds: Rectangle
        get() = Rectangle.fromCenter(position, radius * 2.0, radius * 2.0)

}

class Link(
    var source: Int,
    var target: Int,
)

class Triangle(var a: Int, var b: Int, var c: Int) {
    fun signedArea(nodes: List<Node>): Double {
        val p0 = nodes[a].position
        val p1 = nodes[b].position
        val p2 = nodes[c].position

        return 0.5 * ((p1.x - p0.x) * (p2.y - p0.y)
                - (p2.x - p0.x) * (p1.y - p0.y));
    }
}

interface Constraint {
    suspend fun initialize()
    suspend fun solve(body: Body, dt: Double)
}

interface Force {
    suspend fun initializeFrame(body: Body)
    suspend fun apply(body: Body, dt: Double)
}

interface InterbodyForce {
    suspend fun initializeFrame()
    suspend fun findOverlappingPairs(bodies: List<Body>): List<Pair<Int, Int>>
    suspend fun apply(body: Body, other: Body, dt: Double, substep: Int)
}

interface CollisionConstraint {
    suspend fun solve(body: Body, other: Body, dt: Double)
}

interface BroadPhaseCollisionDetector {
    suspend fun findOverlappingPairs(bodies: List<Body>): List<Pair<Int, Int>>
}

class Body(
    val nodes: List<Node>,
    val boundaryNodes: List<Int> = emptyList(),
    val links: List<Link> = emptyList(),
    val boundaryLinks: List<Int> = emptyList(),
    val triangles: List<Triangle> = emptyList(),
    var inverseBodyMass: Double = 1.0,
    var static: Boolean = false
) {
    var forces = mutableListOf<Force>()
    var constraints = mutableListOf<Constraint>()

    var bounds = Rectangle.EMPTY

    init {
        initializeWeights()
    }

    fun updateBounds() {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY

        for (node in nodes) {
            minX = minOf(minX, node.position.x - node.radius)
            maxX = maxOf(maxX, node.position.x + node.radius)
            minY = minOf(minY, node.position.y - node.radius)
            maxY = maxOf(maxY, node.position.y + node.radius)
        }
        bounds = Rectangle(minX, minY, maxX - minX, maxY - minY)
    }

    suspend fun presolve(dt: Double) {
        for (force in forces) {
            force.apply(this, dt)
        }
    }

    suspend fun solve(dt: Double) {

        for (i in nodes.indices) {
            nodes[i].prevPosition = nodes[i].position

            if (!static) {
                nodes[i].position += nodes[i].velocity * dt
            }
        }

        for (constraint in constraints) {
            constraint.solve(this, dt)
        }
    }

    fun postsolve(dt: Double) {
        for (i in nodes.indices) {
            nodes[i].velocity = (nodes[i].position - nodes[i].prevPosition) / dt
        }
    }

    suspend fun initialize() {
        for (force in forces) {
            force.initializeFrame(this)
        }
    }

    fun initializeWeights() {
        if (triangles.isNotEmpty()) {
            for (node in nodes) {
                node.inverseMass = 0.0
            }
        } else {
            for (node in nodes) {
                node.inverseMass = nodes.size * inverseBodyMass
            }
        }

        for (i in triangles.indices) {
            val a = abs(triangles[i].signedArea(nodes))
            val invMass = (1.0 / (a / 3.0)) * inverseBodyMass
            nodes[triangles[i].a].inverseMass += invMass
            nodes[triangles[i].b].inverseMass += invMass
            nodes[triangles[i].c].inverseMass += invMass
        }
    }
}

class ForceSimulation(val bodies: MutableList<Body> = mutableListOf()) {

    var broadPhaseCollisionDetector: BroadPhaseCollisionDetector? = null
    var collisionConstraint: CollisionConstraint? = null

    val interbodyForces = mutableListOf<InterbodyForce>()

    var context: CoroutineContext = Dispatchers.Default

    private fun partitionPairs(pairs: List<Pair<Int, Int>>): List<List<Pair<Int, Int>>> {
        val batches = mutableListOf<MutableList<Pair<Int, Int>>>()
        val usedInBatch = mutableListOf<MutableSet<Int>>()

        for (pair in pairs) {
            var batchIndex = 0
            while (batchIndex < batches.size) {
                if (pair.first !in usedInBatch[batchIndex] && pair.second !in usedInBatch[batchIndex]) {
                    break
                }
                batchIndex++
            }

            if (batchIndex == batches.size) {
                batches.add(mutableListOf())
                usedInBatch.add(mutableSetOf())
            }
            batches[batchIndex].add(pair)
            usedInBatch[batchIndex].add(pair.first)
            usedInBatch[batchIndex].add(pair.second)
        }
        return batches
    }


    suspend fun simulate(dt: Double, substeps: Int = 10) {
        coroutineScope {
            withContext(context) {
                val sdt = dt / substeps

                for (f in interbodyForces) {
                    f.initializeFrame()
                }

                bodies.map { body ->
                    async {
                        for (force in body.forces) {
                            force.initializeFrame(body)
                        }
                    }
                }.awaitAll()

                for (i in 0 until substeps) {
                    bodies.map { body ->
                        async {
                            body.presolve(sdt)
                        }
                    }.awaitAll()

                    for (force in interbodyForces) {
                        val pairs = force.findOverlappingPairs(bodies)
                        val batches = partitionPairs(pairs)

                        for (batch in batches) {
                            batch.map { pair ->
                                async {
                                    force.apply(bodies[pair.first], bodies[pair.second], sdt, i)
                                }
                            }.awaitAll()
                        }
                    }

                    bodies.map { body ->
                        async {
                            body.solve(sdt)
                        }
                    }.awaitAll()

                    for (body in bodies) {
                        body.updateBounds()
                    }

                    val intersectingPairs = broadPhaseCollisionDetector?.findOverlappingPairs(bodies) ?: emptyList()
                    val collisionBatches = partitionPairs(intersectingPairs)

                    for (batch in collisionBatches) {
                        batch.map { pair ->
                            async {
                                collisionConstraint?.solve(bodies[pair.first], bodies[pair.second], sdt)
                            }
                        }.awaitAll()
                    }

                    for (body in bodies) {
                        body.postsolve(sdt)
                    }
                }
            }
        }
    }
}
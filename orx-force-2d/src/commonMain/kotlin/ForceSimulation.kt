package org.openrndr.extra.force2d
import kotlinx.coroutines.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

/**
 * Represents a single physical node with position, velocity, and related properties.
 *
 * A node serves as an element in simulations, with attributes including its current position,
 * previous position, velocity, inverse mass, radius, and a unique identifier. These properties
 * define the motion and interaction of the node with forces or other nodes in the simulation.
 *
 * @property position the current position of the node in the simulation space.
 * @property prevPosition the position of the node in the previous simulation step.
 * @property velocity the current velocity of the node.
 * @property inverseMass the reciprocal of the node's mass. A value of 0 represents an immovable object,
 * while higher values represent lower mass.
 * @property radius the radius of the node, used for interactions or spatial calculations.
 * @property id an optional unique identifier for the node. Default is 0.
 * @property bounds a bounding rectangle centered around the node, with dimensions based on its radius.
 */
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

/**
 * Represents a connection or relationship between two [Node]s, identified by their indices.
 *
 * This class is primarily used to model a connection between two nodes or objects in a system,
 * such as a physical simulation or graph structure. The `source` and `target` properties represent
 * the indices of the connected nodes.
 *
 * @property source the index of the source node.
 * @property target the index of the target node.
 */
class Link(
    var source: Int,
    var target: Int,
)

/**
 * Represents a triangle defined by three node indices in a physical simulation.
 *
 * A Triangle is primarily used to perform calculations related to its geometry,
 * such as computing its signed area based on the positions of nodes in a given list.
 *
 * @constructor Creates a Triangle with indices pointing to nodes in a list.
 * @param a the index of the first node in the triangle.
 * @param b the index of the second node in the triangle.
 * @param c the index of the third node in the triangle.
 */
class Triangle(var a: Int, var b: Int, var c: Int) {

    /**
     * Computes the signed area of the triangle defined by the nodes referenced in this triangle.
     * The signed area is positive if the nodes are ordered counterclockwise,
     * and negative if they are ordered clockwise.
     *
     * @param nodes a list of [Node] objects where this triangle's node indices correspond to the nodes in the list.
     * Each [Node] must have a valid position.
     * @return the signed area of the triangle defined by the referenced nodes.
     */
    fun signedArea(nodes: List<Node>): Double {
        val p0 = nodes[a].position
        val p1 = nodes[b].position
        val p2 = nodes[c].position

        return 0.5 * ((p1.x - p0.x) * (p2.y - p0.y)
                - (p2.x - p0.x) * (p1.y - p0.y));
    }
}

/**
 * Represents a constraint in a physical system, which influences the behavior of a [Body].
 *
 * A [Constraint] typically applies rules or restrictions to the motion or interaction of
 * the elements within a [Body]. This can include conditions like maintaining distances
 * between points, restricting movement to certain boundaries, or other physical interactions.
 */
interface Constraint {
    suspend fun initialize()

    /**
     * Resolves constraints and updates the state of nodes within the given physical body over a time step.
     *
     * This method applies all constraints associated with the [Body], ensuring that the system adheres
     * to the defined physical rules. For example, constraints might enforce distance limitations,
     * maintain structural rigidity, or apply boundary conditions. It leverages the constraint-solving
     * functionality for the provided time interval, potentially resulting in updated positions or states
     * of the body's components.
     *
     * @param body the [Body] whose constraints are to be resolved, representing the physical system.
     * @param dt the duration of the time step during which the constraints are solved, expressed in seconds.
     */
    suspend fun solve(body: Body, dt: Double)
}

/**
 * Represents a physical force that can be applied to a [Body] in a simulation.
 *
 * Implementing classes define specific behaviors for initializing and
 * applying forces on the [Body], altering the state of its nodes over time.
 */
interface Force {
    suspend fun initializeFrame(body: Body)
    suspend fun apply(body: Body, dt: Double)
}

/**
 * Interface representing a force interaction between two physical bodies in a simulation.
 *
 * Implementations of this interface define the logic for detecting overlapping pairs of bodies,
 * initializing frames for force calculations, and applying interbody forces during simulation steps.
 */
interface InterbodyForce {
    suspend fun initializeFrame()
    suspend fun findOverlappingPairs(bodies: List<Body>): List<Pair<Int, Int>>
    suspend fun apply(body: Body, other: Body, dt: Double, substep: Int)
}

/**
 * An interface representing a constraint that resolves collisions between two bodies in a simulation.
 *
 * Implementations of this interface must define the logic for resolving collisions between the specified
 * [body] and [other] based on their physical properties, positions, and velocities. This process
 * ensures that the bodies interact realistically while respecting the constraints defined for the simulation.
 *
 * The [solve] method is called with a time step [dt] to calculate and apply necessary adjustments to
 * the bodies involved in the collision.
 */
interface CollisionConstraint {
    suspend fun solve(body: Body, other: Body, dt: Double)
}

/**
 * Interface for detecting broad-phase collisions in a physics simulation.
 *
 * The broad-phase collision detector is responsible for identifying potential overlapping
 * pairs of bodies in a physics simulation. It typically uses efficient spatial partitioning
 * or bounding volume techniques to narrow down the list of potential collisions from
 * a large number of bodies.
 */
interface BroadPhaseCollisionDetector {
    suspend fun findOverlappingPairs(bodies: List<Body>): List<Pair<Int, Int>>
}

/**
 * Represents a physical body in a simulation composed of nodes, links, and triangles.
 *
 * A [Body] serves as the central structure in simulations, encapsulating physical properties,
 * relationships, and behaviors of its constituent parts. It supports initialization,
 * integration of forces, resolution of constraints, and updates to its boundaries.
 * The body can either be static or dynamic, depending on the `static` property.
 *
 * @property nodes the list of [Node]s that make up the physical body.
 * @property boundaryNodes the list of indices referring to nodes considered as boundary nodes.
 * @property links the list of [Link]s that define relationships or connections between nodes.
 * @property boundaryLinks the list of indices referring to links considered as boundary links.
 * @property triangles the list of [Triangle]s used for structural and mass-related calculations.
 * @property inverseBodyMass the reciprocal of the total mass of the body, used in weight calculations.
 * @property static a flag indicating whether the body is static (non-movable) or dynamic.
 * @property forces the list of [Force]s acting on the body in the simulation.
 * @property constraints the list of [Constraint]s influencing the behavior and interaction of the body.
 * @property bounds the bounding rectangle that encapsulates the body's nodes, updated automatically.
 */
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

/**
 * Represents a physics simulation of interacting bodies.
 *
 * The [ForceSimulation] class provides the infrastructure to simulate the dynamics
 * of a system of bodies under the influence of forces and constraints. It supports
 * customizable interbody forces, collision detection, and resolution, allowing for
 * a wide variety of physical interactions.
 *
 * @property bodies The list of [Body] objects participating in the simulation. Each body
 * has its own properties, forces, and constraints that determine its behavior.
 * @property broadPhaseCollisionDetector The collision detection algorithm used to identify
 * potential overlapping pairs of bodies during the simulation. It operates in the broad-phase
 * stage of collision resolution.
 * @property collisionConstraint The collision resolution handler responsible for determining
 * the outcome of detected collisions between bodies.
 * @property interbodyForces A list of interbody forces that affect pairs of bodies in the simulation.
 * These forces are computed per pair of bodies and influence their dynamics.
 * @property context The coroutine context in which the simulation runs, allowing for asynchronous
 * computations. By default, it uses [Dispatchers.Default].
 */
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
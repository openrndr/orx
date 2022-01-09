package org.openrndr.extra.computegraph

import kotlinx.coroutines.*
import mu.KotlinLogging
import org.openrndr.events.Event
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger { }

data class ComputeEvent(val source: ComputeNode)

open class ComputeNode(val graph: ComputeGraph, var computeFunction: suspend () -> Unit = {}) {
    internal var updateFunction = {}

    var inputs = mutableMapOf<String, Any>()
    val outputs = mutableMapOf<String, Any>()
    var name = "unnamed-node-${this.hashCode()}"
    private var lastInputsHash = inputs.hashCode()
    var receivedComputeRequest = true
    private val computeFinished = Event<Unit>("compute-finished")

    fun needsRecompute(): Boolean {
        return receivedComputeRequest || (inputs.hashCode() != lastInputsHash)
    }

    fun dependOn(node: ComputeNode) {
        graph.nodes.add(this)
        graph.inbound.getOrPut(this) { mutableSetOf() }.add(node)
        graph.outbound.getOrPut(node) { mutableSetOf() }.add(node)

        node.computeFinished.listen {
            receivedComputeRequest = true
            graph.requireCompute.add(this)
            computeFinished.trigger(Unit)
        }
    }

    /**
     * Set an update function, this function is called unconditionally by the compute-graph processor. This update
     * function can be used to change values of [inputs] to trigger compute of the node.
     */
    fun update(updateFunction: () -> Unit) {
        this.updateFunction = updateFunction
    }

    fun compute(computeFunction: suspend () -> Unit) {
        this.computeFunction = computeFunction
    }

    suspend fun execute() {
        receivedComputeRequest = false
        lastInputsHash = inputs.hashCode()
        computeFunction()
        computeFinished.trigger(Unit)
    }

    override fun toString(): String {
        return "ComputeNode(name='$name', receivedComputeRequest=$receivedComputeRequest)"
    }
}

class ComputeGraph {
    val root = ComputeNode(this, {})
    internal val requireCompute = ArrayDeque<ComputeNode>()

    val nodes = mutableListOf<ComputeNode>()
    val inbound = mutableMapOf<ComputeNode, MutableSet<ComputeNode>>()
    val outbound = mutableMapOf<ComputeNode, MutableSet<ComputeNode>>()

    var job: Job? = null
    fun node(builder: ComputeNode.() -> Unit): ComputeNode {
        val cn = ComputeNode(this)
        cn.builder()
        return cn
    }

    private var computeHash = -1

    /**
     * Run the compute graph in [context].
     *
     * Eventually we likely want to separate compute-graph definitions from the compute-graph processor.
     */
    fun dispatch(context: CoroutineDispatcher, delayBeforeCompute: Long = 500) {
        var firstRodeo = true
        GlobalScope.launch(context, CoroutineStart.DEFAULT) {
            while (true) {
                for (node in nodes) {
                    node.updateFunction()
                }
                val testHash = nodes.map { it.inputs.hashCode() }.reduce { acc, computeNode -> acc * 31 + computeNode }
                if (testHash != computeHash) {
                    logger.info { "canceling job $job" }
                    job?.cancel()
                    job = null
                }
                if (testHash != computeHash && job == null) {
                    computeHash = testHash
                    job = GlobalScope.launch(context) {
                        if (!firstRodeo) {
                            delay(delayBeforeCompute)
                        }
                        logger.info { "compute started" }
                        compute()
                        logger.info { "compute finished" }
                        firstRodeo = false
                    }
                }
                yield()
            }
        }
    }

    suspend fun compute() {
        for (node in nodes) {
            if (node.needsRecompute()) {
                if (node !in requireCompute) {
                    logger.info { "node '${node.name}' needs computation" }
                    requireCompute.add(node)
                }
            }
        }
        val processed = mutableListOf<ComputeNode>()
        root.receivedComputeRequest = false
        while (requireCompute.isNotEmpty()) {
            val node = requireCompute.first {
                val deps = (inbound[it] ?: emptyList())
                if (deps.isEmpty()) {
                    true
                } else {
                    deps.none { dep -> dep in requireCompute }
                }
            }
            requireCompute.remove(node)
            if (node !in processed) {
                logger.info { "computing ${node.name}" }
                node.execute()
                processed.add(node)
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun computeGraph(builder: ComputeGraph.() -> Unit): ComputeGraph {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    val cg = ComputeGraph()
    cg.builder()
    return cg
}


class MutableMapKeyReference<T : Any>(private val map: MutableMap<String, Any>, private val key: String) {
    operator fun getValue(any: Any?, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return map[key] as T
    }

    operator fun setValue(any: Any?, property: KProperty<*>, value: Any) {
        @Suppress("UNCHECKED_CAST")
        map[key] = value as T
    }
}

/**
 * Create a map delegation by [key]
 */
fun <T : Any> MutableMap<String, Any>.withKey(key: String): MutableMapKeyReference<T> {
    return MutableMapKeyReference(this, key)
}
import org.openrndr.extra.style.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestSelectors {
    class Node(
        val type: String, val id: String? = null, val classes: Set<String> = emptySet(),
        val attributes: Map<String, String> = emptyMap(), val states: Set<String> = emptySet(),
        vararg children: Node
    ) {
        var parent: Node? = null
        val children = children.toList().onEach { it.parent = this }
        override fun toString() = type + (id?.let { "#$it" } ?: "")
    }

    object Tree : SelectorTree<Node> {
        override fun parent(node: Node) = node.parent
        override fun children(node: Node) = node.children
        override fun type(node: Node) = node.type
        override fun id(node: Node) = node.id
        override fun hasClass(node: Node, name: String) = name in node.classes
        override fun attribute(node: Node, name: String) = node.attributes[name]
        override fun hasState(node: Node, name: String) = name in node.states
    }

    private val a = Node("rect", "a", setOf("tile", "on"), mapOf("data-kind" to "big box"))
    private val b = Node("circle", "b", setOf("tile"))
    private val c = Node("rect", "c", setOf("tile"), states = setOf("hover"))
    private val d = Node("text", "d")
    private val group = Node("g", "group", setOf("row"), emptyMap(), setOf("hover"), a, b, c, d)
    private val root = Node("svg", "root", emptySet(), emptyMap(), emptySet(), group)

    private fun selects(selector: String): List<String> {
        val parsed = assertNotNull(parseCssSelector(selector), selector)
        val all = generateSequence(listOf(root)) { level -> level.flatMap { it.children }.takeIf { it.isNotEmpty() } }.flatten()
        return all.filter { parsed.matches(it, Tree) }.mapNotNull { it.id }.toList()
    }

    @Test
    fun selectsByTypeIdClassAndAttribute() {
        assertEquals(listOf("a", "c"), selects("rect"))
        assertEquals(listOf("a", "c"), selects("RECT"))
        assertEquals(listOf("b"), selects("#b"))
        assertEquals(listOf("a"), selects("rect.tile.on"))
        assertEquals(listOf("root", "group", "a", "b", "c", "d"), selects("*"))
        assertEquals(listOf("a"), selects("[data-kind]"))
        assertEquals(listOf("a"), selects("[data-kind~=box]"))
        assertEquals(listOf("a"), selects("[data-kind^='big']"))
        assertEquals(listOf("a"), selects("[data-kind=\"BIG BOX\" i]"))
        assertEquals(emptyList(), selects("[data-kind=big]"))
    }

    @Test
    fun combinators() {
        assertEquals(listOf("a", "b", "c"), selects(".row .tile"))
        assertEquals(listOf("a", "b", "c"), selects("svg .tile"))
        assertEquals(emptyList(), selects("svg > .tile"))
        assertEquals(listOf("group"), selects("svg>g"))
        assertEquals(listOf("b"), selects("rect + circle"))
        assertEquals(listOf("c"), selects("circle ~ rect"))
        assertEquals(listOf("d"), selects(".on ~ text"))
    }

    @Test
    fun pseudoClasses() {
        assertEquals(listOf("root"), selects(":root"))
        assertEquals(listOf("a"), selects(".tile:first-child"))
        assertEquals(listOf("d"), selects("g > :last-child"))
        assertEquals(listOf("a", "c"), selects("g > :nth-child(odd)"))
        assertEquals(listOf("b", "d"), selects("g > :nth-child(2n)"))
        assertEquals(listOf("c"), selects("rect:nth-of-type(2)"))
        assertEquals(listOf("c", "d"), selects("g > :nth-last-child(-n + 2)"))
        assertEquals(listOf("c"), selects(".tile:hover"))
        assertEquals(listOf("a", "b", "c"), selects("g:hover .tile"))
        assertEquals(listOf("b", "c"), selects(".tile:not(.on)"))
        assertEquals(listOf("a", "b"), selects(":is(circle, .on)"))
        assertEquals(listOf("d"), selects("text:only-of-type"))
    }

    @Test
    fun specificity() {
        assertEquals(Specificity(1, 2, 1), parseCssSelector("g#x .a:hover")!!.specificity)
        assertEquals(Specificity(0, 1, 1), parseCssSelector(":is(rect, .a) circle")!!.specificity)
        assertEquals(Specificity(0, 0, 1), parseCssSelector(":where(#x) circle")!!.specificity)
        assertTrue(Specificity(1, 0, 0) > Specificity(0, 9, 9))
    }

    @Test
    fun dependencies() {
        assertTrue(parseCssSelector("a + b")!!.usesSiblings)
        assertTrue(parseCssSelector("li:nth-child(2)")!!.usesSiblings)
        assertFalse(parseCssSelector("g .a")!!.usesSiblings)
        assertEquals(setOf("hover", "focus"), parseCssSelector("g:hover .a:not(:focus)")!!.states)
    }

    @Test
    fun invalidAndUnsupportedSelectors() {
        assertNull(parseCssSelector(""))
        assertNull(parseCssSelector("a >"))
        assertNull(parseCssSelector("p::before"))
        assertNull(parseCssSelector("div:has(p)"))
        assertNull(parseCssSelector("[a=]"))
        assertEquals(2, parseCssSelectorList("rect, g > .a:not(.b, .c)")!!.size)
        assertEquals("g > .a:not(.b, .c)", parseCssSelector("g>.a:not(.b,.c)").toString())
    }
}

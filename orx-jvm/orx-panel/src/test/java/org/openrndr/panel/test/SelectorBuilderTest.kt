//package org.openrndr.panel.test
//
//import net.lustlab.panel.elements.Element
//import net.lustlab.panel.elements.ElementClass
//import net.lustlab.panel.elements.ElementType
//import net.lustlab.panel.style.*
//import org.jetbrains.spek.api.Spek
//import org.jetbrains.spek.api.dsl.describe
//import org.jetbrains.spek.api.dsl.it
//import kotlin.test.assertEquals
//import kotlin.test.assertFalse
//import kotlin.test.assertNotNull
//import kotlin.test.assertTrue
//
///**
// * Created by voorbeeld on 11/20/16.
// */
//class SomeTest : Spek({
//
//    describe("a thing") {
//
//        // .panel > button
//        val cs = selector(class_="panel") withChild selector(type="button", class_="fancy")
//
//        val root = Element(ElementType("body"))
//        val panel = Element(ElementType("div")).apply {
//            classes+= ElementClass("panel")
//        }
//        val button = Element(ElementType("button"))
//        val button2 = Element(ElementType("button")).apply {
//            classes+= ElementClass("fancy")
//        }
//
//        root.append(panel)
//        panel.append(button)
//        panel.append(button2)
//
//        it("should work") {
//            assert(cs.selectors.size == 1)
//            assertTrue(cs.selectors[0] is TypeSelector)
//            assertNotNull(cs.previous)
//
//            assertFalse(Matcher().matches(cs, button))
//            assertFalse(Matcher().matches(cs, panel))
//            assertTrue(Matcher().matches(cs, button2))
//        }
//
//        it("should have precedences") {
//            println(cs.precedence())
//
//
//        }
//
//    }
//
//})
//

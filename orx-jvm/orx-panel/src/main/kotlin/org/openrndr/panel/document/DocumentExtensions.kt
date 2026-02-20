package org.openrndr.panel.document

import org.openrndr.panel.elements.Body

fun document(init: Document.() -> Unit) = Document().apply(init)

fun Document.body(init: Body.() -> Unit) = body.apply(init)
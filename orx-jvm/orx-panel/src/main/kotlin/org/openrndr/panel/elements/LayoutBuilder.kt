package org.openrndr.panel.elements

import org.openrndr.draw.Drawer
import org.openrndr.panel.ControlManager
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun Element.layout(init: Element.() -> Unit) {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    init()
}

@OptIn(ExperimentalContracts::class)
fun layout(controlManager: ControlManager, init: Body.() -> Unit): Body {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val body = Body(controlManager)
    body.init()
    return body
}

@OptIn(ExperimentalContracts::class)
fun <T : Element> Element.initElement(classes: Array<out String>, element: T, init: T.() -> Unit): T {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    append(element)
    element.classes.addAll(classes.map { ElementClass(it) })
    element.init()
    return element
}

@OptIn(ExperimentalContracts::class)
fun Element.button(vararg classes: String, label: String = "button", init: Button.() -> Unit): Button {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val button = Button().apply {
        this.classes.addAll(classes.map { ElementClass(it) })
        this.id = id
        this.label = label
    }
    initElement(classes, button, init)
    return button
}

fun Button.clicked(listener: (Button.ButtonEvent) -> Unit) {
    events.clicked.listen(listener)
}

@OptIn(ExperimentalContracts::class)
fun Element.slider(vararg classes: String, init: Slider.() -> Unit) : Slider {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, Slider(), init)
}

@OptIn(ExperimentalContracts::class)
fun Element.toggle(vararg classes: String, init: Toggle.() -> Unit): Toggle {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, Toggle(), init)
}

@OptIn(ExperimentalContracts::class)
fun Element.colorpicker(vararg classes: String, init: Colorpicker.() -> Unit): Colorpicker {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, Colorpicker(), init)
}

@OptIn(ExperimentalContracts::class)
fun Element.colorpickerButton(vararg classes: String, init: ColorpickerButton.() -> Unit): ColorpickerButton {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, ColorpickerButton(), init)
}


@OptIn(ExperimentalContracts::class)
fun Element.xyPad(vararg classes: String, init: XYPad.() -> Unit): XYPad {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, XYPad(), init)
}

fun Canvas.draw(f: (Drawer) -> Unit) {
    this.userDraw = f
}

@OptIn(ExperimentalContracts::class)
fun Element.canvas(vararg classes: String, init: Canvas.() -> Unit): Canvas {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val canvas = Canvas()
    classes.forEach { canvas.classes.add(ElementClass(it)) }
    canvas.init()
    append(canvas)
    return canvas
}

@OptIn(ExperimentalContracts::class)
fun Element.dropdownButton(
    vararg classes: String,
    id: String? = null,
    label: String = "button",
    init: DropdownButton.() -> Unit
): DropdownButton {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, DropdownButton().apply {
        this.id = id
        this.label = label
    }, init)
}

@OptIn(ExperimentalContracts::class)
fun Element.envelopeButton(vararg classes: String, init: EnvelopeButton.() -> Unit): EnvelopeButton {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, EnvelopeButton().apply {}, init)
}

@OptIn(ExperimentalContracts::class)
fun Element.envelopeEditor(vararg classes: String, init: EnvelopeEditor.() -> Unit): EnvelopeEditor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, EnvelopeEditor().apply {}, init)
}

@OptIn(ExperimentalContracts::class)
fun Element.sequenceEditor(vararg classes: String, init: SequenceEditor.() -> Unit): SequenceEditor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, SequenceEditor().apply {}, init)
}
@OptIn(ExperimentalContracts::class)
fun Element.slidersVector2(vararg classes: String, init: SlidersVector2.() -> Unit): SlidersVector2 {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, SlidersVector2().apply {}, init)
}


@OptIn(ExperimentalContracts::class)
fun Element.slidersVector3(vararg classes: String, init: SlidersVector3.() -> Unit): SlidersVector3 {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, SlidersVector3().apply {}, init)
}

@OptIn(ExperimentalContracts::class)
fun Element.slidersVector4(vararg classes: String, init: SlidersVector4.() -> Unit): SlidersVector4 {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, SlidersVector4().apply {}, init)
}


@OptIn(ExperimentalContracts::class)
fun Element.textfield(vararg classes: String, init: Textfield.() -> Unit): Textfield {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return initElement(classes, Textfield(), init)
}

@OptIn(ExperimentalContracts::class)
fun DropdownButton.item(init: Item.() -> Unit): Item {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val item = Item().apply(init)
    append(item)
    return item
}

@OptIn(ExperimentalContracts::class)
fun Element.div(vararg classes: String, init: Div.() -> Unit): Div {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val div = Div()
    initElement(classes, div, init)
    return div
}

@OptIn(ExperimentalContracts::class)
inline fun <reified T : TextElement> Element.textElement(classes: Array<out String>, init: T.() -> String): T {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    @Suppress("DEPRECATION") val te = T::class.java.newInstance()
    te.classes.addAll(classes.map { ElementClass(it) })
    te.text(te.init())
    append(te)
    return te
}

@OptIn(ExperimentalContracts::class)
fun Element.p(vararg classes: String, init: P.() -> String): P {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return textElement(classes, init)
}
@OptIn(ExperimentalContracts::class)
fun Element.h1(vararg classes: String, init: H1.() -> String): H1 {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return textElement(classes, init)
}
@OptIn(ExperimentalContracts::class)
fun Element.h2(vararg classes: String, init: H2.() -> String): H2 {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return textElement(classes, init)
}

@OptIn(ExperimentalContracts::class)
fun Element.h3(vararg classes: String, init: H3.() -> String): H3 {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return textElement(classes, init)
}


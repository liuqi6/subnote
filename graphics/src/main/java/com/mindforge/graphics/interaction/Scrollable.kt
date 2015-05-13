package com.mindforge.graphics.interaction

import com.mindforge.graphics.*
import com.mindforge.graphics.math.*

class Scrollable(val element: Element<*>) : Composed<Any?>, PointersElement<Any?> {
    override val content: Any? get() = element.content
    override val changed = trigger<Unit>()
    var scrollPosition = zeroVector2
    override val elements: ObservableIterable<TransformedElement<*>> = observableIterable(listOf(object : TransformedElement<Any?> {
        override val element: Element<Any?> = this@Scrollable.element
        override val transform: Transform2 get() = Transforms2.translation(scrollPosition)
        override val transformChanged = this@Scrollable.changed
    }, transformedElement(coloredElement(rectangle(vector(10000, 10000)), Fills.solid(Colors.white)))))
    private var lastLocation: Vector2? = null

    override fun onPointerKeyPressed(pointerKey: PointerKey) {
        lastLocation = pointerKey.pointer.location
    }

    override fun onPointerKeyReleased(pointerKey: PointerKey) {
        lastLocation = null
    }

    override fun onPointerMoved(pointer: Pointer) {
        val last = lastLocation
        val current = pointer.location
        if (last != null) {
            scrollPosition += current - last
            lastLocation = current
            changed()
        }
    }
}
package justitone

import org.apache.commons.math3.fraction.BigFraction

import java.util.ArrayList
import java.util.Arrays

class Sequence {
    var events: MutableList<Event>

    constructor() {
        events = ArrayList()
    }

    constructor(sequence: Sequence) {
        events = ArrayList()
        events.addAll(sequence.events)
    }

    constructor(events: List<Event>) {
        this.events = ArrayList()
        this.events.addAll(events)
    }

    constructor(vararg events: Event) {
        this.events = ArrayList()
        this.events.addAll(Arrays.asList(*events))
    }

    fun addEvent(e: Event) {
        events.add(e)
    }

    fun length(): BigFraction {
        return events.map { e -> e.length() }
                     .reduce{ obj, fraction -> obj.add(fraction) ?: BigFraction.ZERO
    }

    fun contents(): List<Event> {
        return events
    }
}

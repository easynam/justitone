package justitone

import justitone.util.*
import org.apache.commons.math3.fraction.BigFraction

abstract class Event {
    abstract val tokens: Set<TokenPos>

    abstract fun length(): BigFraction

    abstract fun multiply(other: Event): Event

    data class PolyGroup(
            override val tokens: Set<TokenPos> = emptySet(),
            val children: List<Group> = emptyList()
    ) : Event() {
        override fun length(): BigFraction {
            return children
                    .map(Event::length)
                    .max() ?: BigFraction.ZERO
        }

        override fun multiply(other: Event): PolyGroup {
            return copy(
                    tokens = this.tokens union other.tokens,
                    children = children.map { e -> e.multiply(other) })
        }
    }

    data class Group (
            override val tokens: Set<TokenPos> = emptySet(),
            val children: List<Event> = emptyList()
    ) : Event() {
        override fun length(): BigFraction {
            return children
                    .map(Event::length)
                    .fold(BigFraction.ZERO, BigFraction::plus)
        }

        override fun multiply(other: Event): Group {
            return Group(
                    tokens = this.tokens union other.tokens,
                    children = children.map { e -> e.multiply(other) }
            )
        }
    }

    data class Leaf (
            override val tokens: Set<TokenPos> = emptySet(),
            val length: BigFraction = BigFraction.ONE,
            val ratio: BigFraction = BigFraction.ONE,
            val octaves: BigFraction = BigFraction.ZERO,
            val noteOn: Boolean = true
    ) : Event() {
        private fun multiplyLeaf(other: Leaf): Event {
            return Leaf(
                    tokens = this.tokens union other.tokens,
                    length = length * other.length,
                    ratio = ratio * other.ratio,
                    octaves = octaves + other.octaves,
                    noteOn = noteOn && other.noteOn
            )
        }

        override fun length(): BigFraction = length

        override fun multiply(other: Event): Event {
            return if (other is Leaf) {
                multiplyLeaf(other)
            } else {
                return other.multiply(this)
            }
        }
    }
}

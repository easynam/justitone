package justitone

import java.util.ArrayList

import org.apache.commons.math3.fraction.BigFraction

abstract class Event {
    val tokens: MutableList<TokenPos> = ArrayList()
    open val ratio: BigFraction = BigFraction.ONE
    protected open val length: BigFraction = BigFraction.ONE

    fun withTokenPos(token: TokenPos): Event {
        tokens.add(token)
        return this
    }

    open fun length(): BigFraction {
        return length
    }

    open fun chop(toLength: BigFraction): SubSequence {
        return Event.Bar(toLength.divide(length()), BigFraction.ONE, Sequence(this))
    }

    abstract class SubSequence : Event() {
        abstract val sequence: Sequence
        abstract fun eventLength(): BigFraction
    }

    data class Note(override val length: BigFraction = BigFraction.ONE, override val ratio: BigFraction = BigFraction.ONE) : Event()
    data class Rest(override val length: BigFraction = BigFraction.ONE) : Event()
    data class Hold(override val length: BigFraction = BigFraction.ONE) : Event()
    data class Modulation(override val ratio: BigFraction) : Event()
    data class Instrument(val instrument: Int) : Event()

    data class Tuple(override val length: BigFraction = BigFraction.ONE,
                     override val ratio: BigFraction = BigFraction.ONE,
                     override val sequence: Sequence) : SubSequence() {
        override fun eventLength(): BigFraction {
            return length.divide(sequence.length())
        }

        override fun chop(toLength: BigFraction): SubSequence {
            val newTupleLength = toLength.divide(length)
            val innerLength = toLength.divide(eventLength())
            var total = BigFraction.ZERO

            val seq = Sequence()

            for (e in sequence.events) {
                val start = total
                total = total.add(e.length())

                if (total >= innerLength) {
                    seq.addEvent(e.chop(innerLength.subtract(start)))

                    break
                }

                seq.addEvent(e)
            }

            return Tuple(newTupleLength, ratio, seq)
        }
    }

    data class Bar(val eventLength: BigFraction = BigFraction.ONE,
                   override val ratio: BigFraction = BigFraction.ONE,
                   override val sequence: Sequence) : SubSequence() {
        override fun eventLength(): BigFraction {
            return eventLength
        }

        override fun chop(toLength: BigFraction): SubSequence {
            val innerLength = toLength.divide(eventLength())

            var total = BigFraction.ZERO

            val seq = Sequence()

            for (e in sequence.events) {
                val start = total
                total = total.add(e.length())

                if (total >= innerLength) {
                    seq.addEvent(e.chop(innerLength.subtract(start)))

                    break
                }

                seq.addEvent(e)
            }

            return Bar(eventLength, ratio, seq)
        }
    }

    data class Poly(override val ratio: BigFraction, val sequences: List<SubSequence>) : Event() {
        override fun length(): BigFraction {
            return sequences.map{ it.length() }.max() ?: BigFraction.ZERO
        }

        override fun chop(toLength: BigFraction): SubSequence {
            val seqs = sequences.map { s -> s.chop(toLength) }

            return Bar(sequence = Sequence(Poly(BigFraction.ONE, seqs)))
        }
    }
}

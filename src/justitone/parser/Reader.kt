package justitone.parser

import justitone.Event
import justitone.Event.SubSequence
import justitone.Sequence
import justitone.Song
import justitone.TokenPos
import justitone.antlr.JIBaseVisitor
import justitone.antlr.JILexer
import justitone.antlr.JIParser
import justitone.antlr.JIParser.EventContext
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.apache.commons.math3.fraction.BigFraction
import java.util.*

class Reader {
    internal val songVisitor = SongVisitor()
    internal val defVisitor = DefVisitor()
    internal val sequenceVisitor = SequenceVisitor()
    internal val polySequenceVisitor = PolySequenceVisitor()
    internal val sequenceItemVisitor = SequenceItemVisitor()
    internal val eventVisitor = EventVisitor()
    internal val fractionVisitor = FractionVisitor()
    internal val pitchVisitor = PitchVisitor()
    internal val integerVisitor = IntegerVisitor()

    internal val defines: MutableMap<String, EventContext> = HashMap()

    fun parse(source: String): Song {
        defines.clear()

        val charStream = CharStreams.fromString(source)
        val lexer = JILexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = JIParser(tokens)

        return songVisitor.visit(parser.song())
    }

    internal inner class SongVisitor : JIBaseVisitor<Song>() {
        override fun visitSong(ctx: JIParser.SongContext): Song {
            ctx.def().forEach { def -> def.accept(defVisitor) }
            val tempo = ctx.tempo.accept(integerVisitor)
            val seq = ctx.sequence().accept(sequenceVisitor)

            return Song(seq, tempo)
        }
    }

    internal inner class DefVisitor : JIBaseVisitor<Void>() {
        override fun visitDef(ctx: JIParser.DefContext): Void? {
            val identifier = ctx.identifier().text

            defines[identifier] = ctx.event()

            return null
        }
    }

    internal inner class SequenceVisitor : JIBaseVisitor<Sequence>() {
        override fun visitSequence(ctx: JIParser.SequenceContext): Sequence {
            val seq = Sequence()

            ctx.sequenceItem().stream()
                    .map { event -> event.accept(sequenceItemVisitor) }
                    .forEach { it(seq) }

            return seq
        }
    }

    internal inner class PolySequenceVisitor : JIBaseVisitor<List<Sequence>>() {
        override fun visitPolySequence(ctx: JIParser.PolySequenceContext): List<Sequence> {

            return ctx.sequence()
                    .map { event -> event.accept(sequenceVisitor) }
        }
    }

    internal inner class SequenceItemVisitor : JIBaseVisitor<(Sequence) -> Unit>() {
        override fun visitEventRepeat(ctx: JIParser.EventRepeatContext): (Sequence) -> Unit {
            val repeats = if (ctx.repeats == null) 1 else ctx.repeats.accept(integerVisitor)

            val event = ctx.event().accept(eventVisitor)

            return { s ->
                for (i in 0 until repeats) {
                    println("adding $event")
                    s.addEvent(event)
                }
            }
        }

        override fun visitJump(ctx: JIParser.JumpContext): (Sequence) -> Unit {
            val length = if (ctx.lengthMultiplier == null) BigFraction.ONE else ctx.lengthMultiplier.accept(fractionVisitor)
            val ratio = if (ctx.pitch() == null) BigFraction.ONE else ctx.pitch().accept(pitchVisitor)
            val repeats = if (ctx.times == null) 1 else ctx.times.accept(integerVisitor)

            return {
                var sequence = it
                for (i in 0 until repeats) {
                    val bar = Sequence(it)

                    sequence.addEvent(Event.Bar(length, ratio, bar))

                    sequence = bar
                }
            }
        }
    }

    internal inner class EventVisitor : JIBaseVisitor<Event>() {
        override fun visitEventNote(ctx: JIParser.EventNoteContext): Event {
            val length = if (ctx.length == null) BigFraction.ONE else ctx.length.accept(fractionVisitor)
            val ratio = if (ctx.pitch() == null) BigFraction.ONE else ctx.pitch().accept(pitchVisitor)

            return Event.Note(length, ratio).withTokenPos(tokenPos(ctx))
        }

        override fun visitEventRest(ctx: JIParser.EventRestContext): Event {
            val length = if (ctx.length == null) BigFraction.ONE else ctx.length.accept(fractionVisitor)

            return Event.Rest(length).withTokenPos(tokenPos(ctx))
        }

        override fun visitEventHold(ctx: JIParser.EventHoldContext): Event {
            val length = if (ctx.length == null) BigFraction.ONE else ctx.length.accept(fractionVisitor)

            return Event.Hold(length).withTokenPos(tokenPos(ctx))
        }

        override fun visitEventTuple(ctx: JIParser.EventTupleContext): Event {
            val length = if (ctx.length == null) BigFraction.ONE else ctx.length.accept(fractionVisitor)
            val ratio = if (ctx.pitch() == null) BigFraction.ONE else ctx.pitch().accept(pitchVisitor)
            val sequences = ctx.polySequence().accept(polySequenceVisitor)

            val tuples = sequences
                    .map { s -> Event.Tuple(length, ratio, s) }

            return Event.Poly(ratio, tuples)
        }

        override fun visitEventFill(ctx: JIParser.EventFillContext): Event {
            //this should go somewhere else at this point
            val length = if (ctx.lengthMultiplier == null) BigFraction.ONE else ctx.lengthMultiplier.accept(fractionVisitor)
            val ratio = if (ctx.pitch() == null) BigFraction.ONE else ctx.pitch().accept(pitchVisitor)

            val start = if (ctx.start == null) Sequence() else ctx.start.accept(sequenceVisitor)
            val loop = ctx.loop.accept(sequenceVisitor)

            if (loop.length() == BigFraction.ZERO) {
                throw RuntimeException("Empty loop in fill at " + ctx.loop.start.startIndex)
            }

            if (start.length() >= length) {
                return Event.Bar(sequence = start).chop(length)
            }

            var total = start.length()
            val seq = Sequence()

            println("CREATED SEQ------$seq")

            seq.addEvent(Event.Bar(sequence = start))

            while (true) {
                val prev = total
                total = total.add(loop.length())

                if (total >= length) {
                    val toLength = length.subtract(prev)

                    seq.addEvent(Event.Bar(sequence = loop).chop(toLength))

                    break
                }

                seq.addEvent(Event.Bar(sequence = loop))
            }

            return Event.Bar(BigFraction.ONE, ratio, seq)
        }

        override fun visitEventBar(ctx: JIParser.EventBarContext): Event {
            val length = if (ctx.lengthMultiplier == null) BigFraction.ONE else ctx.lengthMultiplier.accept(fractionVisitor)
            val ratio = if (ctx.pitch() == null) BigFraction.ONE else ctx.pitch().accept(pitchVisitor)
            val sequences = ctx.polySequence().accept(polySequenceVisitor)

            val bars = sequences
                    .map { s -> Event.Bar(length, ratio, s) }

            return Event.Poly(ratio, bars)
        }

        override fun visitEventModGroup(ctx: JIParser.EventModGroupContext): Event {
            val length = if (ctx.lengthMultiplier == null) BigFraction.ONE else ctx.lengthMultiplier.accept(fractionVisitor)
            val ratio = if (ctx.pitch() == null) BigFraction.ONE else ctx.pitch().accept(pitchVisitor)
            val sequences = ctx.polySequence().accept(polySequenceVisitor)

            val bars = sequences
                    .map { it.events.flatMap { listOf(it, Event.Modulation(it.ratio)) } }
                    .map { Event.Bar(length, ratio, Sequence(it)) }

            return Event.Poly(ratio, bars)
        }

        override fun visitEventChord(ctx: JIParser.EventChordContext): Event {
            val event = if (ctx.event() == null) Event.Note().withTokenPos(tokenPos(ctx)) else ctx.event().accept(eventVisitor).withTokenPos(tokenPos(ctx))
            var ratio = BigFraction.ONE

            val pitches = ctx.pitch()
                    .map { p -> p.accept(pitchVisitor) }

            val events = ArrayList<SubSequence>()

            for (p in pitches) {
                ratio = ratio.multiply(p)
                events.add(Event.Bar(BigFraction.ONE, ratio, Sequence(event)))
            }

            return Event.Poly(event.ratio, events)
        }

        override fun visitEventModulation(ctx: JIParser.EventModulationContext): Event {
            val ratio = ctx.pitch().accept(pitchVisitor)

            return Event.Modulation(ratio).withTokenPos(tokenPos(ctx))
        }

        override fun visitEventInstrument(ctx: JIParser.EventInstrumentContext): Event {
            val instrument = ctx.integer().accept(integerVisitor) - 1

            return Event.Instrument(instrument).withTokenPos(tokenPos(ctx))
        }

        override fun visitEventDef(ctx: JIParser.EventDefContext): Event {
            val length = if (ctx.length == null) BigFraction.ONE else ctx.length.accept(fractionVisitor)
            val ratio = if (ctx.pitch() == null) BigFraction.ONE else ctx.pitch().accept(pitchVisitor)

            val identifier = ctx.identifier().text

            return Event.Bar(length, ratio, Sequence(defines[identifier]!!.accept(eventVisitor).withTokenPos(tokenPos(ctx))))
        }
    }

    internal inner class PitchVisitor : JIBaseVisitor<BigFraction>() {
        fun invert(ratio: BigFraction, shouldInvert: Boolean): BigFraction {
            return if (shouldInvert) {
                ratio.reciprocal()
            } else
                ratio
        }

        override fun visitPitchRatio(ctx: JIParser.PitchRatioContext): BigFraction {
            val invert = ctx.MINUS() != null

            return invert(ctx.ratio.accept(fractionVisitor), invert)
        }

        override fun visitPitchSuperparticular(ctx: JIParser.PitchSuperparticularContext): BigFraction {
            val numerator = ctx.integer().accept(integerVisitor)
            val invert = ctx.MINUS() != null

            if (numerator <= 1) {
                throw RuntimeException("Superparticular numerator cant be less than 2")
            }

            return invert(BigFraction(numerator, numerator - 1), invert)
        }


        override fun visitPitchAngle(ctx: JIParser.PitchAngleContext): BigFraction {
            val angle = ctx.integer().accept(integerVisitor)
            val invert = ctx.MINUS() != null

            return invert(BigFraction(180, 180 - angle), invert)
        }

        override fun visitPitchMultiple(ctx: JIParser.PitchMultipleContext): BigFraction {
            return ctx.pitch()
                    .map { it.accept(pitchVisitor) }
                    .reduce { obj, fraction -> obj.multiply(fraction) }
        }

        override fun visitPitchPower(ctx: JIParser.PitchPowerContext): BigFraction {
            val pitch = ctx.pitch().accept(pitchVisitor)
            return pitch.pow(ctx.PLUS().size + 1)
        }
    }

    internal inner class FractionVisitor : JIBaseVisitor<BigFraction>() {
        override fun visitFraction(ctx: JIParser.FractionContext): BigFraction {
            val numerator = if (ctx.num == null) 1 else ctx.num.accept(integerVisitor)
            val denominator = if (ctx.den == null) 1 else ctx.den.accept(integerVisitor)

            return BigFraction(numerator, denominator)
        }
    }

    internal inner class IntegerVisitor : JIBaseVisitor<Int>() {
        override fun visitInteger(ctx: JIParser.IntegerContext): Int? {
            return Integer.parseInt(ctx.text)
        }
    }

    companion object {

        fun tokenPos(ctx: ParserRuleContext): TokenPos {
            return TokenPos(ctx.start.startIndex, ctx.stop.stopIndex)
        }
    }
}

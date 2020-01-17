package justitone.parser

import justitone.Event
import justitone.Song
import justitone.TokenPos
import justitone.antlr.JIv2Lexer
import justitone.antlr.JIv2Parser
import justitone.antlr.JIv2Parser.EventContext
import justitone.antlr.JIv2BaseVisitor
import justitone.util.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.apache.commons.math3.fraction.BigFraction
import java.util.*

class Reader {
    internal val songVisitor = SongVisitor()
//    internal val defVisitor = DefVisitor()
    internal val polyGroupVisitor = PolyGroupVisitor()
    internal val groupVisitor = GroupVisitor()
    internal val eventVisitor = EventVisitor()
    internal val fractionVisitor = FractionVisitor()
    internal val pitchVisitor = PitchVisitor()
    internal val integerVisitor = IntegerVisitor()

    internal val defines: MutableMap<String, EventContext> = HashMap()

    fun parse(source: String): Song {
        defines.clear()

        val charStream = CharStreams.fromString(source)
        val lexer = JIv2Lexer(charStream)
        val tokens = CommonTokenStream(lexer)
        val parser = JIv2Parser(tokens)

        return songVisitor.visit(parser.song())
    }

    internal inner class SongVisitor : JIv2BaseVisitor<Song>() {
        override fun visitSong(ctx: JIv2Parser.SongContext): Song {
            val tempo = ctx.tempo.accept(integerVisitor)
            val seq = ctx.event().accept(eventVisitor)

            return Song(seq, tempo)
        }
    }

    internal inner class GroupVisitor : JIv2BaseVisitor<Event.Group>() {
        override fun visitGroup(ctx: JIv2Parser.GroupContext): Event.Group {
            return Event.Group(
                    tokens = setOf(tokenPos(ctx)),
                    children = ctx.event().map { it.accept(eventVisitor) }
            )
        }
    }

    internal inner class PolyGroupVisitor : JIv2BaseVisitor<Event.PolyGroup>() {
        override fun visitPolyGroup(ctx: JIv2Parser.PolyGroupContext): Event.PolyGroup {
            return Event.PolyGroup(
                    tokens = setOf(tokenPos(ctx)),
                    children = ctx.group().map { it.accept(groupVisitor) }
            )
        }
    }

    internal inner class EventVisitor : JIv2BaseVisitor<Event>() {
        override fun visitEventPitch(ctx: JIv2Parser.EventPitchContext): Event {
            val ratio = ctx.pitch().accept(pitchVisitor)

            return Event.Leaf(
                    tokens = setOf(tokenPos(ctx)),
                    ratio = ratio
            )
        }

        override fun visitEventDuration(ctx: JIv2Parser.EventDurationContext): Event {
            val length = ctx.fraction().accept(fractionVisitor)

            return Event.Leaf(
                    tokens = setOf(tokenPos(ctx)),
                    length = length
            )
        }

        override fun visitEventGroup(ctx: JIv2Parser.EventGroupContext): Event {
            return ctx.polyGroup().accept(polyGroupVisitor)
        }

        override fun visitEventMultiplied(ctx: JIv2Parser.EventMultipliedContext): Event {
            return ctx.event()
                    .map { it.accept(eventVisitor) }
                    .reduce { first, second -> second.multiply(first) }
        }

        override fun visitEventOctave(ctx: JIv2Parser.EventOctaveContext): Event {
            val octaves = ctx.fraction().accept(fractionVisitor)

            return Event.Leaf(
                    tokens = setOf(tokenPos(ctx)),
                    octaves = octaves
            )
        }
    }

    internal inner class PitchVisitor : JIv2BaseVisitor<BigFraction>() {
        override fun visitPitchRatio(ctx: JIv2Parser.PitchRatioContext): BigFraction {
            return ctx.ratio.accept(fractionVisitor)
        }

        override fun visitPitchSuperparticular(ctx: JIv2Parser.PitchSuperparticularContext): BigFraction {
            val numerator = ctx.integer().accept(integerVisitor)

            if (numerator <= 1) {
                throw RuntimeException("Superparticular numerator can't be less than 1")
            }

            return numerator over (numerator - 1)
        }

        override fun visitPitchZero(ctx: JIv2Parser.PitchZeroContext): BigFraction {
            return BigFraction.ZERO
        }

        override fun visitPitchReciprocal(ctx: JIv2Parser.PitchReciprocalContext): BigFraction {
            return ctx.pitch().accept(pitchVisitor).reciprocal()
        }
    }

    internal inner class FractionVisitor : JIv2BaseVisitor<BigFraction>() {
        override fun visitFraction(ctx: JIv2Parser.FractionContext): BigFraction {
            val numerator = ctx.num?.accept(integerVisitor) ?: 1
            val denominator = ctx.den?.accept(integerVisitor) ?: 1

            return numerator over denominator
        }
    }

    internal inner class IntegerVisitor : JIv2BaseVisitor<Int>() {
        override fun visitInteger(ctx: JIv2Parser.IntegerContext): Int? {
            return Integer.parseInt(ctx.text)
        }
    }

    companion object {

        fun tokenPos(ctx: ParserRuleContext): TokenPos {
            return TokenPos(ctx.start.startIndex, ctx.stop.stopIndex)
        }
    }
}

package justitone.jidi

import java.util.stream.Collectors

import org.apache.commons.math3.fraction.BigFraction

import justitone.Event
import justitone.Sequence
import justitone.Song

//java intonation digital interface
class JidiSequence(song: Song, ppq: Int) {
    var tracks: MutableList<JidiTrack> = mutableListOf()
    private val used: MutableMap<JidiTrack, Periods> = mutableMapOf()

    val ppm: Int =  ppq * 4;
    val bpm: Int = song.bpm;

    init {
        val track = allocateTrack(BigFraction.ZERO, song.sequence.length())

        loadSequence(State(), BigFraction.ZERO, false, song.sequence, track)
    }

    private fun allocateTrack(start: BigFraction, end: BigFraction): JidiTrack {
        val track = tracks.asSequence()
                .filter { t -> used[t]!!.canAllocate(start, end) }
                .firstOrNull()
        return if (track != null) {
            used[track]!!.allocate(start, end)

            track
        } else {
            val newTrack = JidiTrack(tracks.size)
            val periods = Periods()
            periods.allocate(start, end)
            tracks.add(newTrack)
            used[newTrack] = periods

            newTrack
        }
    }

    private fun loadSequence(state: State, currentPos: BigFraction, noteOn: Boolean, sequence: Sequence, track: JidiTrack) {
        var state = state
        var currentPos = currentPos
        var noteOn = noteOn

        track.add(JidiEvent.Instrument(currentPos.multiply(ppm).toInt().toLong(), state.instrument))

        for (e in sequence.contents()) {
            val tick = currentPos.multiply(ppm).toLong()

            when (e) {
                is Event.Note -> {
                    track.add(JidiEvent.Token(tick, e.tokens))

                    if (noteOn) {
                        track.add(JidiEvent.NoteOff(tick))
                    }

                    val freq = e.ratio.multiply(state.freqMultiplier).toFloat() * 440f

                    track.add(JidiEvent.Pitch(tick, freq))
                    track.add(JidiEvent.NoteOn(tick))

                    noteOn = true
                }
                is Event.Rest -> {
                    track.add(JidiEvent.Token(tick, e.tokens))

                    if (noteOn) {
                        track.add(JidiEvent.NoteOff(tick))
                    }

                    noteOn = false
                }
                is Event.Hold -> track.add(JidiEvent.Token(tick, e.tokens))
                is Event.Modulation -> state = state.multiplyFreq(e.ratio)
                is Event.Instrument -> {
                    if (noteOn) {
                        track.add(JidiEvent.NoteOff(tick))
                    }

                    val (instrument) = e
                    track.add(JidiEvent.Instrument(tick, instrument))

                    state = state.copy(instrument = e.instrument)

                    noteOn = false
                }
                is Event.SubSequence -> loadSequence(state.multiplyLength(e.eventLength()).multiplyFreq(e.ratio),
                                                     currentPos, noteOn, e.sequence, track)
                is Event.Poly -> loadPoly(state, currentPos, noteOn, e.sequences, track)
            }

            currentPos = currentPos.add(e.length().multiply(state.lengthMultiplier))
        }

        val tick = currentPos.multiply(ppm).toLong()

        track.add(JidiEvent.Token(tick, emptyList()))
        if (noteOn) {
            track.add(JidiEvent.NoteOff(currentPos.multiply(ppm).toInt().toLong()))
        }
    }

    private fun loadPoly(state: State, currentPos: BigFraction, noteOn: Boolean, subs: List<Event.SubSequence>, track: JidiTrack) {
        var sub: Event.SubSequence = subs[0]

        loadSequence(state.multiplyLength(sub.eventLength()).multiplyFreq(sub.ratio),
                currentPos, noteOn, subs[0].sequence, track)

        if (subs.size > 1) {
            for (i in 1 until subs.size) {
                sub = subs[i]

                val end = currentPos.add(sub.length().multiply(state.lengthMultiplier))

                loadSequence(state.multiplyLength(sub.eventLength()).multiplyFreq(sub.ratio),
                        currentPos, false, sub.sequence, allocateTrack(currentPos, end))
            }
        }
    }

    private data class State (val lengthMultiplier: BigFraction = BigFraction.ONE, val freqMultiplier: BigFraction = BigFraction.ONE, val instrument: Int = 0) {
        fun multiplyLength(multiplier: BigFraction): State = copy(lengthMultiplier = lengthMultiplier.multiply(multiplier))
        fun multiplyFreq(multiplier: BigFraction): State = copy(freqMultiplier = freqMultiplier.multiply(multiplier))
    }
}

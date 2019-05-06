package justitone.jidi

import justitone.Event
import justitone.Sequence
import justitone.Song
import justitone.util.plus
import justitone.util.times
import org.apache.commons.math3.fraction.BigFraction

//java intonation digital interface
class JidiSequence(song: Song = Song(Sequence(), 123), ppq: Int = 768) {
    var tracks: MutableList<JidiTrack> = mutableListOf()
    private val used: MutableMap<JidiTrack, Periods> = mutableMapOf()

    val ppm: Int = ppq * 4;
    val bpm: Int = song.bpm;

    init {
        val track = allocateTrack(BigFraction.ZERO, song.sequence.length())

        loadSequence(State(), song.sequence, track)
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

    private fun loadSequence(state: State, sequence: Sequence, track: JidiTrack) {
        var state = state

        track.add(JidiEvent.Instrument(state.currentTick(ppm), state.instrument))

        for (e in sequence.contents()) {
            val tick = state.currentTick(ppm);

            when (e) {
                is Event.Note -> {
                    track.add(JidiEvent.Token(tick, e.tokens))

                    if (state.noteOn) {
                        track.add(JidiEvent.NoteOff(tick))
                    }

                    val freq = (e.ratio * state.freqMultiplier).toFloat() * 440f

                    track.add(JidiEvent.Pitch(tick, freq))
                    track.add(JidiEvent.NoteOn(tick))

                    state = state.copy(noteOn = true)
                }
                is Event.Rest -> {
                    track.add(JidiEvent.Token(tick, e.tokens))

                    if (state.noteOn) {
                        track.add(JidiEvent.NoteOff(tick))

                        state = state.copy(noteOn = false)
                    }
                }
                is Event.Hold -> track.add(JidiEvent.Token(tick, e.tokens))
                is Event.Modulation -> state = state.multiplyFreq(e.ratio)
                is Event.Instrument -> {
                    if (state.noteOn) {
                        track.add(JidiEvent.NoteOff(tick))
                    }

                    val (instrument) = e
                    track.add(JidiEvent.Instrument(tick, instrument))

                    state = state.copy(noteOn = false, instrument = e.instrument)
                }
                is Event.SubSequence -> loadSequence(state.multiplyLength(e.eventLength()).multiplyFreq(e.ratio), e.sequence, track)
                is Event.Poly -> loadPoly(state, e.sequences, track)
            }

            state = state.advancePos(e.length() * state.lengthMultiplier)
        }

        val tick = state.currentTick(ppm);

        track.add(JidiEvent.Token(tick, emptyList()))
        if (state.noteOn) {
            track.add(JidiEvent.NoteOff(tick))
        }
    }

    private fun loadPoly(state: State, subs: List<Event.SubSequence>, track: JidiTrack) {
        var sub: Event.SubSequence = subs[0]

        loadSequence(state.multiplyLength(sub.eventLength()).multiplyFreq(sub.ratio), subs[0].sequence, track)

        if (subs.size > 1) {
            for (i in 1 until subs.size) {
                sub = subs[i]

                val end = state.currentPos.add(sub.length() * state.lengthMultiplier)

                loadSequence(state.multiplyLength(sub.eventLength()).multiplyFreq(sub.ratio), sub.sequence, allocateTrack(state.currentPos, end))
            }
        }
    }

    private data class State(val currentPos: BigFraction = BigFraction.ZERO,
                             val noteOn: Boolean = false,
                             val lengthMultiplier: BigFraction = BigFraction.ONE,
                             val freqMultiplier: BigFraction = BigFraction.ONE,
                             val instrument: Int = 0) {
        fun multiplyLength(multiplier: BigFraction): State = copy(lengthMultiplier = lengthMultiplier * multiplier)
        fun multiplyFreq(multiplier: BigFraction): State = copy(freqMultiplier = freqMultiplier * multiplier)
        fun currentTick(ppm: Int): Long = (currentPos * ppm).toLong()
        fun advancePos(time: BigFraction): State = copy(currentPos = currentPos + time)
    }
}

package justitone.jidi

import justitone.Event
import justitone.Song
import justitone.util.plus
import justitone.util.times
import org.apache.commons.math3.fraction.BigFraction
import java.lang.RuntimeException

//java intonation digital interface
class JidiSequence(song: Song = Song(Event.Group(), 120), ppq: Int = 768) {
    var tracks: MutableList<JidiTrack> = mutableListOf()
    private val used: MutableMap<JidiTrack, Periods> = mutableMapOf()

    val ppm: Int = ppq * 4;
    val bpm: Int = song.bpm;

    init {
        val track = allocateTrack(BigFraction.ZERO, song.event.length())

        loadEvent(State(), song.event, track)
    }

    private fun allocateTrack(start: BigFraction, end: BigFraction): JidiTrack {
        val track = tracks.asSequence()
                .filter { t -> t.periods.canAllocate(start, end) }
                .firstOrNull()
        return if (track != null) {
            track.periods.allocate(start, end)

            track
        } else {
            val newTrack = JidiTrack(tracks.size)
            newTrack.periods.allocate(start, end)
            tracks.add(newTrack)

            newTrack
        }
    }

    private fun loadEvent(state: State, e: Event, track: JidiTrack) : State {
        val tick = state.currentTick(ppm);

//        track.add(JidiEvent.Instrument(state.currentTick(ppm), state.instrument))

        return when (e) {
            is Event.Leaf -> {
                track.add(JidiEvent.Token(tick, e.tokens))

                if (state.noteOn) {
                    track.add(JidiEvent.NoteOff(tick))
                }

                if (e.ratio == BigFraction.ZERO) {
                    state.advancePos(e.length())
                } else {
                    val freq = e.ratio.toFloat() * 440f

                    track.add(JidiEvent.Pitch(tick, freq))
                    track.add(JidiEvent.NoteOn(tick))

                    state.copy(noteOn = true).advancePos(e.length())
                }
            }
            is Event.Group -> {
                if (state.noteOn) {
                    track.add(JidiEvent.NoteOff(tick))
                }

                val newState = e.children.fold(state.copy(noteOn = false)) { state, event ->
                    loadEvent(state, event, track)
                }

                track.add(JidiEvent.Token(newState.currentTick(ppm), emptySet()))
                if (newState.noteOn) {
                    track.add(JidiEvent.NoteOff(newState.currentTick(ppm)))
                }

                newState
            }
            is Event.PolyGroup -> {
                if (state.noteOn) {
                    track.add(JidiEvent.NoteOff(tick))
                }

                loadPoly(state.copy(noteOn = false), e, track)

                state.advancePos(e.length())
            }
            else -> {
                throw RuntimeException()
            }
        }
    }

    private fun loadPoly(state: State, poly: Event.PolyGroup, track: JidiTrack) {
        var event = poly.children[0]

        loadEvent(state, event, track)

        if (poly.children.size > 1) {
            for (i in 1 until poly.children.size) {
                event = poly.children[i]

                val end = state.currentPos.add(event.length())

                loadEvent(state, event, allocateTrack(state.currentPos, end))
            }
        }
    }

    private data class State(val currentPos: BigFraction = BigFraction.ZERO,
                             val noteOn: Boolean = false) {
        fun currentTick(ppm: Int): Long = (currentPos * ppm).toLong()
        fun advancePos(time: BigFraction): State = copy(currentPos = currentPos + time)
    }
}

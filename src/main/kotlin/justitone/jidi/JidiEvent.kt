package justitone.jidi

import justitone.TokenPos

sealed class JidiEvent {
    abstract val tick: Long

    data class NoteOn(override val tick: Long) : JidiEvent()
    data class NoteOff(override val tick: Long) : JidiEvent()
    data class Pitch(override val tick: Long, val freq: Float) : JidiEvent()
    data class Instrument(override val tick: Long, val instrument: Int) : JidiEvent()
    data class Token(override val tick: Long, val tokens: List<TokenPos>) : JidiEvent() {
        val start by lazy { tokens.map { it.start }.min() ?: 0 }
        val stop by lazy { tokens.map { it.stop }.max() ?: 0 }
    }
}

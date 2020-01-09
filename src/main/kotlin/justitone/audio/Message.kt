package justitone.audio

import justitone.jidi.JidiSequence

sealed class Message {
    data class SetSequence(val sequence: JidiSequence) : Message()
    data class SetTick(val tick: Long) : Message()
    object Play : Message()
    object Stop : Message()
}
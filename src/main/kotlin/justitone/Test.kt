package justitone

import justitone.jidi.JidiSequence
import justitone.midi.Midi
import justitone.parser.Reader

import javax.sound.midi.InvalidMidiDataException
import javax.sound.midi.MidiSystem
import javax.sound.sampled.LineUnavailableException
import java.io.File
import java.io.IOException

object Test {
    @Throws(LineUnavailableException::class, InvalidMidiDataException::class, IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val reader = Reader()

        val song = reader.parse("120: ^:/243 [:243 :244 :245 :246 :247 :248 :249 :250 :251 :252 :253 :254 :255 :256]")

        val seq = JidiSequence(song, 768)

        val midi = Midi()

        val midiSeq = midi.jidiToMidi(seq, 2f)

        val f = File("midifile.mid")
        MidiSystem.write(midiSeq, 1, f)
    }
}

package justitone.midi

import justitone.jidi.JidiEvent
import justitone.jidi.JidiSequence
import justitone.jidi.JidiTrack

import javax.sound.midi.*

class Midi {
    @Throws(InvalidMidiDataException::class)
    fun jidiToMidi(jidi: JidiSequence, pitchBendRange: Float): Sequence {
        val seq = Sequence(Sequence.PPQ, jidi.ppm / 4)

        val tempoTrack = seq.createTrack()

        //tempo
        run {
            val message = MetaMessage()
            message.setMessage(0x51, encodeTempo(jidi.bpm), 3)
            tempoTrack.add(MidiEvent(message, 0))
        }
        //i forgot. todo extract into function with name that makes sense
        run {
            val message = ShortMessage()
            message.setMessage(0xB0, 126, 0)
            tempoTrack.add(MidiEvent(message, 0))
        }

        var channel = 0

        for (jidiTrack in jidi.tracks) {
            if (channel == 9) channel++
            addTrack(seq, jidiTrack, pitchBendRange, channel++)
        }

        return seq
    }

    private fun note(freq: Float): Float {
        return (69 + 12 * Math.log((freq / 440).toDouble()) / Math.log(2.0)).toFloat()
    }

    private fun midiNote(note: Float): Int {
        return Math.round(note)
    }

    private fun midiPitchBend(note: Float, range: Float): ByteArray {
        val translated = note - midiNote(note)

        val clamped = Math.min(Math.max(translated / range, -1f), 1f)
        val asInt = (clamped * 8192).toInt()

        return encode(asInt)
    }

    private fun encode(num: Int): ByteArray {
        val translated = num + 8192

        val encoded = ByteArray(2)

        encoded[0] = (translated and 0x7F).toByte()
        encoded[1] = (translated and 0x3F80 shr 7).toByte()

        return encoded
    }

    private fun encodeTempo(bpm: Int): ByteArray {
        val micros = 60000000f.toInt() / bpm

        val encoded = ByteArray(3)

        encoded[2] = (micros and 0xFF).toByte()
        encoded[1] = (micros and 0xFF00 shr 8).toByte()
        encoded[0] = (micros and 0xFF0000 shr 16).toByte()

        return encoded
    }

    @Throws(InvalidMidiDataException::class)
    private fun addTrack(seq: Sequence, jidiTrack: JidiTrack, pitchBendRange: Float, channel: Int): Track {
        val track = seq.createTrack()

        val message = ShortMessage()
        message.setMessage(ShortMessage.PROGRAM_CHANGE or channel, 0, 0)
        track.add(MidiEvent(message, 0))

        var freq = 440f

        var noteOn = false

        for (e in jidiTrack.events) {
            when (e) {
                is JidiEvent.Pitch -> {
                    freq = e.freq
                }
                is JidiEvent.NoteOn -> {
                    if (noteOn) {
                        writeNoteOff(track, freq, e.tick, channel)
                    }

                    writeNoteOn(track, freq, e.tick, pitchBendRange, channel)

                    noteOn = true
                }
                is JidiEvent.NoteOff -> {
                    writeNoteOff(track, freq, e.tick, channel)

                    noteOn = false
                }
                is JidiEvent.Instrument -> {
                    if (noteOn) {
                        writeNoteOff(track, freq, e.tick, channel)
                    }

                    writeInstrument(track, e.tick, channel, e.instrument)

                    noteOn = false
                }
            }
        }

        return track
    }

    @Throws(InvalidMidiDataException::class)
    private fun writeNoteOn(track: Track, freq: Float, tick: Long, pitchBendRange: Float, channel: Int) {
        val note = note(freq)

        var message = ShortMessage()
        message.setMessage(ShortMessage.NOTE_ON or channel, midiNote(note) and 0xff, 0x60)
        track.add(MidiEvent(message, tick))

        val pitchBend = midiPitchBend(note, pitchBendRange)

        message = ShortMessage()
        message.setMessage(ShortMessage.PITCH_BEND or channel, pitchBend[0].toInt(), pitchBend[1].toInt())
        track.add(MidiEvent(message, tick))
    }

    @Throws(InvalidMidiDataException::class)
    private fun writeNoteOff(track: Track, freq: Float, tick: Long, channel: Int) {
        val note = note(freq)

        val message = ShortMessage()
        message.setMessage(ShortMessage.NOTE_OFF or channel, midiNote(note), 0x40)
        track.add(MidiEvent(message, tick))
    }

    @Throws(InvalidMidiDataException::class)
    private fun writeInstrument(track: Track, tick: Long, channel: Int, instrument: Int) {
        val message = ShortMessage()
        message.setMessage(ShortMessage.PROGRAM_CHANGE or channel, instrument, 0)
        track.add(MidiEvent(message, tick))
    }
}

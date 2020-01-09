package justitone.audio

import justitone.jidi.JidiEvent
import justitone.jidi.JidiSequence
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine

class Playback(internal var queue: ConcurrentLinkedQueue<Message>) : Runnable {

    var sequence: JidiSequence = JidiSequence()
        internal set
    var isRunning = true
        internal set
    var isPlaying = false
        internal set
    var tick: Long = 0
        internal set
    private var offset = 0f

    override fun run() {
        val af = AudioFormat(sampleRate.toFloat(), 16, 1, true, true)
        val line: SourceDataLine
        try {
            line = AudioSystem.getSourceDataLine(af)
            line.open(af, bufSize * 2)
        } catch (e: LineUnavailableException) {
            e.printStackTrace(System.out)
            return
        }

        line.start()

        while (isRunning) {
            while (queue.isNotEmpty()) {
                val message = queue.poll()
                when (message) {
                    is Message.SetSequence -> sequence = message.sequence
                    is Message.SetTick -> tick = message.tick
                    Message.Play -> isPlaying = true
                    Message.Stop -> isPlaying = false
                }
            }

            val buf = ByteBuffer.allocate(bufSize * 2)

            // fill buffer with samples
            if (isPlaying) {
                putSequence(buf, sequence);
            }

            line.write(buf.array(), 0, bufSize * 2)
        }

        line.close()
    }

    private fun putSequence(buf: ByteBuffer, sequence: JidiSequence) {
        val length: Float = sequence.ppm.toFloat() * (sequence.bpm / 60f) * (bufSize.toFloat() / sampleRate) / 4f

        for (track in sequence.tracks) {
            for ((e, event) in track.events.withIndex()) {
                if (event is JidiEvent.Pitch) {
                    val start = ticksToSeconds(event.tick.toFloat() - tick.toFloat() - offset, sequence.bpm.toFloat(), sequence.ppm.toFloat())

                    val nextEvent = nextNoteEvent(track.events, e + 1)

                    val end = if (nextEvent.isPresent) {
                        ticksToSeconds(nextEvent.get().tick.toFloat() - tick.toFloat() - offset, sequence.bpm.toFloat(), sequence.ppm.toFloat())
                    } else {
                        ticksToSeconds(length, sequence.bpm.toFloat(), sequence.ppm.toFloat())
                    }

                    val startSample = Math.max(0, Math.ceil((start * sampleRate).toDouble()).toInt())
                    val endSample = Math.min(bufSize, Math.floor((end * sampleRate).toDouble()).toInt() + 1)

                    putSine(buf, startSample, endSample, event, start)
                }
            }
        }

        val lengthWhole: Long = length.toLong()
        val lengthFrac: Float = length - lengthWhole
        val fracSum = offset + lengthFrac

        //todo more functional?
        tick += lengthWhole + fracSum.toLong()
        offset = fracSum - fracSum.toLong()
    }

    private fun putSine(buf: ByteBuffer, startSample: Int, endSample: Int, event: JidiEvent.Pitch, start: Float) {
        for (i in startSample until endSample) {
            val current = buf.getShort(i * 2)
            val phase = 2f * Math.PI.toFloat() * event.freq * (i.toFloat() / sampleRate - start)
            val value = (current + Short.MAX_VALUE.toDouble() * 0.1 * Math.sin(phase.toDouble())).toShort()
            buf.putShort(i * 2, value)
        }
    }

    //handle note on later when pitch and note on are both handled properly
    private fun nextNoteEvent(events: List<JidiEvent>, from: Int): Optional<JidiEvent> {
        return events.subList(from, events.size)
                .stream()
                .filter { e -> e is JidiEvent.NoteOff }
                .findFirst()
    }

    companion object {
        const val sampleRate = 44100
        const val bufSize = 512

        fun start(): ConcurrentLinkedQueue<Message> {
            val queue = ConcurrentLinkedQueue<Message>()
            Thread(Playback(queue)).start()
            return queue
        }

        internal fun ticksToSeconds(tick: Float, bpm: Float, ppm: Float): Float {
            return 4f * 60f * (tick / ppm) / bpm
        }
    }

    // public static void save(Track channel) throws IOException {
    //     int length = channel.notes.stream().mapToInt(n -> n.samples(channel.tempo) * 2).sum();

    //     ByteBuffer buffer = ByteBuffer.allocate(length);

    //     channel.notes.stream().forEach(n -> buffer.put(n.sin(channel.tempo)));

    //     final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
    //     AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(buffer.array()), af, length);

    //     AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new File("justitone output.wav"));
    // }
}

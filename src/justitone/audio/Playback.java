package justitone.audio;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import java.util.concurrent.ConcurrentLinkedQueue;

import justitone.jidi.JidiSequence;
import justitone.jidi.JidiTrack;
import justitone.jidi.JidiEvent;

public class Playback implements Runnable {
    static final int sampleRate = 44100;
    static final int bufSize = 512;

    ConcurrentLinkedQueue<Message> queue;

    JidiSequence sequence;
    boolean running = true;
    boolean playing = false;
    int tick = 0;
    float offset = 0;

    public Playback(ConcurrentLinkedQueue<Message> queue) {
        this.queue = queue;
    }

    public static ConcurrentLinkedQueue<Message> start() {
        ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue();
        (new Thread(new Playback(queue))).start();
        return queue;
    }

    @Override
    public void run() {
        final AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, true);
        SourceDataLine line;
        try {
            line = AudioSystem.getSourceDataLine(af);
            line.open(af, bufSize * 2);
        } catch (LineUnavailableException e) {
            e.printStackTrace(System.out);
            return;
        }

        line.start();

        while (running) {
            // process events
            Message message;
            while ((message = queue.poll()) != null) {
                if (message instanceof Message.SetSequence) {
                    sequence = ((Message.SetSequence)message).sequence;
                } else if (message instanceof Message.Play) {
                    playing = true;
                } else if (message instanceof Message.Stop) {
                    playing = false;
                    tick = 0;
                }
            }

            ByteBuffer buf = ByteBuffer.allocate(bufSize * 2);

            // fill buffer with samples
            if (playing) {
                float length = (float)sequence.ppm * ((float)sequence.bpm / 60) * ((float)bufSize / (float)sampleRate) / 4;

                if (sequence != null) {
                    for (JidiTrack track : sequence.tracks) {
                        int e = 0;
                        for (JidiEvent event : track.events) {
                            if (event instanceof JidiEvent.NoteOn) {
                                JidiEvent.NoteOn note = (JidiEvent.NoteOn)event;
                                float start = ticksToSeconds((float)(note.tick - tick) - offset, sequence.bpm, sequence.ppm);
                                float end;
                                if (e + 1 < track.events.size()) {
                                    end = ticksToSeconds((float)(track.events.get(e + 1).tick - tick) - offset, sequence.bpm, sequence.ppm);
                                } else {
                                    end = ticksToSeconds(length, sequence.bpm, sequence.ppm);
                                }
                                int startSample = Math.max(0, (int)Math.ceil(start * sampleRate));
                                int endSample = Math.min(bufSize, (int)Math.floor(end * sampleRate) + 1);
                                for (int i = startSample; i < endSample; i++) {
                                    short current = buf.getShort(i * 2);
                                    float phase = 2 * (float)Math.PI * note.freq * ((float)i / (float)sampleRate - start);
                                    short value = (short)(current + Short.MAX_VALUE * 0.1 * Math.sin(phase));
                                    buf.putShort(i * 2, value);
                                }
                            }
                            e++;
                        }
                    }
                }

                int lengthWhole = (int)length;
                float lengthFrac = length - (float)lengthWhole;
                float fracSum = offset + lengthFrac;

                tick = tick + lengthWhole + (int)fracSum;
                offset = fracSum - (int)fracSum;
            }

            line.write(buf.array(), 0, bufSize * 2);
        }

        line.close();
    }

    static float ticksToSeconds(float tick, int bpm, int ppm) {
        return 4 * 60 * ((tick / (float)ppm) / (float)bpm);
    }

    // public static void save(Track channel) throws IOException {
    //     int length = channel.notes.stream().mapToInt(n -> n.samples(channel.tempo) * 2).sum();

    //     ByteBuffer buffer = ByteBuffer.allocate(length);

    //     channel.notes.stream().forEach(n -> buffer.put(n.sin(channel.tempo)));

    //     final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
    //     AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(buffer.array()), af, length);

    //     AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new File("justitone output.wav"));
    // }

    // private static void play(SourceDataLine line, Note Note, int tempo) {
    //     line.write(Note.sin(tempo), 0, Note.samples(tempo) * 2);
    // }
}

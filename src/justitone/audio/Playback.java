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

import justitone.Track;
import justitone.Note;
import justitone.jidi.JidiSequence;
import justitone.jidi.JidiTrack;
import justitone.jidi.JidiEvent;

public class Playback implements Runnable {
    static final int fs = 44100;
    static final int bufSize = 256;
    static final int sampleSize = 2;

    static final int ticksPerBeat = 480;

    ConcurrentLinkedQueue<Message> queue;

    JidiSequence sequence;
    boolean running = true;
    boolean playing = false;
    int beat;
    int tick;

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
        final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
        SourceDataLine line;
        try {
            line = AudioSystem.getSourceDataLine(af);
            line.open(af, fs);
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
                    beat = 0;
                    tick = 0;
                }
            }

            ByteBuffer buf = ByteBuffer.allocate(bufSize * sampleSize);

            // fill buffer with samples
            if (sequence != null) {
                for (JidiTrack track : sequence.tracks) {
                    for (JidiEvent event : track.events) {
                        if (event instanceof JidiEvent.NoteOn) {
                            
                        } else if (event instanceof JidiEvent.NoteOff) {

                        } else if (event instanceof JidiEvent.Pitch) {
                            
                        }
                    }
                }
            }

            line.write(buf.array(), 0, bufSize);
        }

        line.close();
    }

    public static void save(Track channel) throws IOException {
        int length = channel.notes.stream().mapToInt(n -> n.samples(channel.tempo) * 2).sum();

        ByteBuffer buffer = ByteBuffer.allocate(length);

        channel.notes.stream().forEach(n -> buffer.put(n.sin(channel.tempo)));

        final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
        AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(buffer.array()), af, length);

        AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new File("justitone output.wav"));
    }

    private static void play(SourceDataLine line, Note Note, int tempo) {
        line.write(Note.sin(tempo), 0, Note.samples(tempo) * 2);
    }
}

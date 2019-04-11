package justitone;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Playback {
    static final int fs = 44100;

    public static void play(Sequence channel) throws LineUnavailableException {
//        final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
//        SourceDataLine line = AudioSystem.getSourceDataLine(af);
//
//        line.open(af, fs);
//        line.start();
//
//        for (Note note : channel.events) {
//            play(line, note, channel.tempo);
//        }
//
//        line.drain();
//        line.close();
    }

    public static void save(Sequence channel) throws IOException {
//        int length = channel.events.stream().mapToInt(n -> n.samples(channel.tempo) * 2).sum();
//
//        ByteBuffer buffer = ByteBuffer.allocate(length);
//
//        channel.events.stream().forEach(n -> buffer.put(n.sin(channel.tempo)));
//
//        final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
//        AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(buffer.array()), af, length);
//
//        AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new File("justitone output.wav"));
    }

    private static void play(SourceDataLine line, Note Note, int tempo) {
        line.write(Note.sin(tempo), 0, Note.samples(tempo) * 2);
    }
}

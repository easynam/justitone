package justitone;

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

public class Playback {
	static final int fs = 44100;

	public static void play(Track channel) throws LineUnavailableException {
		final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
		SourceDataLine line = AudioSystem.getSourceDataLine(af);

		line.open(af, fs);
		line.start();
		
		for (Note note : channel.notes) {
			play (line, note);
		}
		
		line.drain();
		line.close();
	}
	
	public static void save(Track channel) throws IOException {
		int length = channel.notes.stream().mapToInt(n -> n.samples() * 2).sum();
		
		ByteBuffer buffer = ByteBuffer.allocate(length);
		
		channel.notes.stream().forEach(n -> buffer.put(n.sin()));
		
		final AudioFormat af = new AudioFormat(fs, 16, 1, true, true);
		AudioInputStream inputStream = new AudioInputStream(new ByteArrayInputStream(buffer.array()),af,length);

        AudioSystem.write(inputStream, AudioFileFormat.Type.WAVE, new File("justitone output.wav"));
	}

	private static void play(SourceDataLine line, Note Note) {
		line.write(Note.sin(), 0, Note.samples()*2);
	}
}
